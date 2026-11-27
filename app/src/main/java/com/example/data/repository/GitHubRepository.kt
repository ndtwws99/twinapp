package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesStore
import com.example.data.model.GitHubEvent
import com.example.data.model.GitHubIssue
import com.example.data.model.GitHubRelease
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.IssueComment
import com.example.data.model.RateLimitInfo
import com.example.data.model.RepoContentItem
import com.example.data.model.SavedRepoSummary
import com.example.data.remote.GitHubApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val isRateLimited: Boolean = false
    ) : ApiResult<Nothing>()
}

class GitHubRepository(context: Context) {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val preferencesStore = UserPreferencesStore(context.applicationContext, moshi)
    val preferencesFlow: Flow<UserPreferences> = preferencesStore.preferencesFlow

    @Volatile
    private var activeToken: String = ""

    private val _rateLimit = MutableStateFlow<RateLimitInfo?>(null)
    val rateLimit: StateFlow<RateLimitInfo?> = _rateLimit.asStateFlow()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.applicationContext.cacheDir, "github_http_cache"), 25L * 1024L * 1024L))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "GitPulse-Android/1.0")
            val token = activeToken
            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            val response = chain.proceed(requestBuilder.build())
            val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull()
            val limit = response.header("X-RateLimit-Limit")?.toIntOrNull()
            if (remaining != null && limit != null) {
                _rateLimit.value = RateLimitInfo(remaining = remaining, limit = limit)
            }
            response
        }
        .build()

    private val api: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)

    private data class CacheEntry<T>(val value: T, val timestampMs: Long)

    private val repoCache = ConcurrentHashMap<String, CacheEntry<GitHubRepo>>()
    private val searchCache = ConcurrentHashMap<String, CacheEntry<List<GitHubRepo>>>()
    private val readmeCache = ConcurrentHashMap<String, CacheEntry<String>>()
    private val cacheTtlMs = 5L * 60L * 1000L

    fun updateActiveToken(token: String) {
        activeToken = token.trim()
    }

    suspend fun getTrendingRepos(topicQuery: String, forceRefresh: Boolean = false): ApiResult<List<GitHubRepo>> {
        val key = "trending:$topicQuery"
        if (!forceRefresh) {
            searchCache[key]?.takeIf { System.currentTimeMillis() - it.timestampMs < cacheTtlMs }?.let {
                return ApiResult.Success(it.value)
            }
        }
        return safeCall {
            val response = api.searchRepositories(
                query = topicQuery,
                sort = "stars",
                order = "desc",
                perPage = 20
            )
            searchCache[key] = CacheEntry(response.items, System.currentTimeMillis())
            response.items
        }
    }

    suspend fun searchRepositories(query: String): ApiResult<List<GitHubRepo>> = safeCall {
        val trimmed = query.trim()
        if (trimmed.isBlank()) emptyList()
        else api.searchRepositories(query = trimmed, sort = "stars", perPage = 25).items
    }

    suspend fun searchUsers(query: String): ApiResult<List<GitHubUser>> = safeCall {
        val trimmed = query.trim()
        if (trimmed.isBlank()) emptyList()
        else api.searchUsers(query = trimmed, perPage = 25).items
    }

    suspend fun getPublicEvents(): ApiResult<List<GitHubEvent>> = safeCall {
        api.getPublicEvents(perPage = 30)
            .filter { it.type in SUPPORTED_SOCIAL_EVENT_TYPES }
    }

    suspend fun getFollowingEvents(usernames: List<String>): ApiResult<List<GitHubEvent>> {
        val targets = usernames.take(4)
        if (targets.isEmpty()) return ApiResult.Success(emptyList())
        return safeCall {
            coroutineScope {
                targets.map { login ->
                    async {
                        runCatching { api.getUserEvents(login, perPage = 8) }.getOrDefault(emptyList())
                    }
                }.awaitAll()
                    .flatten()
                    .filter { it.type in SUPPORTED_SOCIAL_EVENT_TYPES }
                    .sortedByDescending { it.createdAt.orEmpty() }
                    .take(30)
            }
        }
    }

    suspend fun getRepository(owner: String, repo: String, forceRefresh: Boolean = false): ApiResult<GitHubRepo> {
        val key = "$owner/$repo".lowercase()
        if (!forceRefresh) {
            repoCache[key]?.takeIf { System.currentTimeMillis() - it.timestampMs < cacheTtlMs }?.let {
                return ApiResult.Success(it.value)
            }
        }
        return safeCall {
            val result = api.getRepository(owner, repo)
            repoCache[key] = CacheEntry(result, System.currentTimeMillis())
            result
        }
    }

    suspend fun getReadmeMarkdown(owner: String, repo: String): ApiResult<String> {
        val key = "$owner/$repo".lowercase()
        readmeCache[key]?.takeIf { System.currentTimeMillis() - it.timestampMs < cacheTtlMs }?.let {
            return ApiResult.Success(it.value)
        }
        return safeCall {
            val item = api.getReadme(owner, repo)
            val decoded = decodeBase64Utf8(item.content)
            readmeCache[key] = CacheEntry(decoded, System.currentTimeMillis())
            decoded
        }
    }

    suspend fun getReleases(owner: String, repo: String): ApiResult<List<GitHubRelease>> = safeCall {
        api.getReleases(owner, repo, perPage = 20)
    }

    suspend fun getIssues(owner: String, repo: String): ApiResult<List<GitHubIssue>> = safeCall {
        api.getIssues(owner, repo, state = "open", perPage = 25)
    }

    suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int): ApiResult<List<IssueComment>> = safeCall {
        api.getIssueComments(owner, repo, issueNumber, perPage = 30)
    }

    suspend fun getDirectoryContents(owner: String, repo: String, path: String = ""): ApiResult<List<RepoContentItem>> = safeCall {
        val raw = if (path.isBlank()) {
            api.getRootContents(owner, repo)
        } else {
            api.getDirectoryContents(owner, repo, path)
        }
        raw.sortedWith(compareByDescending<RepoContentItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    suspend fun getDecodedFileContent(owner: String, repo: String, path: String): ApiResult<String> = safeCall {
        val item = api.getFileContent(owner, repo, path)
        val rawText = decodeBase64Utf8(item.content)
        if (path.endsWith(".json", ignoreCase = true)) {
            prettyPrintJson(rawText)
        } else {
            rawText
        }
    }

    suspend fun getUserProfile(username: String): ApiResult<GitHubUser> = safeCall {
        api.getUser(username.trim())
    }

    suspend fun getUserRepos(username: String): ApiResult<List<GitHubRepo>> = safeCall {
        api.getUserRepos(username.trim(), sort = "updated", perPage = 30)
    }

    suspend fun getUserEvents(username: String): ApiResult<List<GitHubEvent>> = safeCall {
        api.getUserEvents(username.trim(), perPage = 25)
            .filter { it.type in SUPPORTED_SOCIAL_EVENT_TYPES }
    }

    suspend fun toggleFollowUser(login: String) {
        preferencesStore.toggleFollowUser(login)
    }

    suspend fun toggleSavedRepo(summary: SavedRepoSummary) {
        preferencesStore.toggleSavedRepo(summary)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        preferencesStore.setDarkMode(enabled)
    }

    suspend fun updateConnection(username: String, token: String) {
        activeToken = token.trim()
        preferencesStore.updateConnection(username, token)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            ApiResult.Success(block())
        } catch (e: HttpException) {
            val code = e.code()
            when (code) {
                403, 429 -> ApiResult.Error(
                    message = "Đã chạm giới hạn GitHub API (HTTP $code). Bạn có thể thêm GitHub Token ở mục Hồ sơ để nâng lên 5.000 lượt/giờ.",
                    isRateLimited = true
                )
                404 -> ApiResult.Error("Không tìm thấy dữ liệu tương ứng trên GitHub (404).")
                401 -> ApiResult.Error("GitHub Token không hợp lệ hoặc đã hết hạn (401). Hãy kiểm tra lại ở mục Hồ sơ.")
                else -> ApiResult.Error("Lỗi máy chủ GitHub (HTTP $code). Vui lòng thử lại sau.")
            }
        } catch (_: IOException) {
            ApiResult.Error("Không thể kết nối tới GitHub API. Kiểm tra kết nối mạng và thử lại.")
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Đã xảy ra lỗi không xác định khi tải dữ liệu GitHub.")
        }
    }

    private fun decodeBase64Utf8(base64Content: String?): String {
        if (base64Content.isNullOrBlank()) return ""
        val sanitized = base64Content.replace("\n", "").replace("\r", "")
        val bytes = Base64.decode(sanitized, Base64.DEFAULT)
        return String(bytes, Charsets.UTF_8)
    }

    private fun prettyPrintJson(raw: String): String {
        val trimmed = raw.trim()
        return try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    companion object {
        private val SUPPORTED_SOCIAL_EVENT_TYPES = setOf(
            "PushEvent",
            "ReleaseEvent",
            "WatchEvent",
            "ForkEvent",
            "IssuesEvent",
            "IssueCommentEvent",
            "PullRequestEvent",
            "CreateEvent"
        )
    }
}
