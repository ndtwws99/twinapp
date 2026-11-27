package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val id: Long = 0L,
    val login: String = "",
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "html_url") val htmlUrl: String = "",
    val name: String? = null,
    val company: String? = null,
    val blog: String? = null,
    val location: String? = null,
    val bio: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "public_gists") val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubLicense(
    val key: String? = null,
    val name: String? = null,
    @Json(name = "spdx_id") val spdxId: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long = 0L,
    val name: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val owner: GitHubUser = GitHubUser(),
    @Json(name = "html_url") val htmlUrl: String = "",
    val description: String? = null,
    val fork: Boolean = false,
    val homepage: String? = null,
    val language: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "watchers_count") val watchersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    val topics: List<String> = emptyList(),
    val license: GitHubLicense? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "pushed_at") val pushedAt: String? = null
) {
    val openGraphImageUrl: String
        get() = "https://opengraph.githubassets.com/1/$fullName"
}

@JsonClass(generateAdapter = true)
data class RepoSearchResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val items: List<GitHubRepo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UserSearchResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val items: List<GitHubUser> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReleaseAsset(
    val id: Long = 0L,
    val name: String = "",
    val size: Long = 0L,
    @Json(name = "download_count") val downloadCount: Int = 0,
    @Json(name = "browser_download_url") val browserDownloadUrl: String = "",
    @Json(name = "content_type") val contentType: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    val id: Long = 0L,
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String = "",
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val author: GitHubUser? = null,
    val assets: List<ReleaseAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class IssueLabel(
    val id: Long = 0L,
    val name: String = "",
    val color: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class PullRequestRef(
    val url: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubIssue(
    val id: Long = 0L,
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val state: String = "open",
    @Json(name = "html_url") val htmlUrl: String = "",
    val comments: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val user: GitHubUser? = null,
    val labels: List<IssueLabel> = emptyList(),
    @Json(name = "pull_request") val pullRequest: PullRequestRef? = null
) {
    val isPullRequest: Boolean
        get() = pullRequest != null
}

@JsonClass(generateAdapter = true)
data class IssueComment(
    val id: Long = 0L,
    val body: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val user: GitHubUser? = null,
    @Json(name = "html_url") val htmlUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class RepoContentItem(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0L,
    val type: String = "file",
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null,
    val content: String? = null,
    val encoding: String? = null
) {
    val isDirectory: Boolean
        get() = type == "dir"
}

@JsonClass(generateAdapter = true)
data class EventActor(
    val id: Long = 0L,
    val login: String = "",
    @Json(name = "display_login") val displayLogin: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class EventRepo(
    val id: Long = 0L,
    val name: String = "",
    val url: String = ""
) {
    val owner: String
        get() = name.substringBefore("/", "")
    val repoName: String
        get() = name.substringAfter("/", name)
}

@JsonClass(generateAdapter = true)
data class EventCommit(
    val sha: String = "",
    val message: String = ""
)

@JsonClass(generateAdapter = true)
data class EventPayload(
    val action: String? = null,
    val ref: String? = null,
    @Json(name = "ref_type") val refType: String? = null,
    val size: Int? = null,
    val commits: List<EventCommit>? = null,
    val release: GitHubRelease? = null,
    val issue: GitHubIssue? = null,
    val comment: IssueComment? = null
)

@JsonClass(generateAdapter = true)
data class GitHubEvent(
    val id: String = "",
    val type: String = "",
    val actor: EventActor = EventActor(),
    val repo: EventRepo = EventRepo(),
    val payload: EventPayload? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class RateLimitInfo(
    val remaining: Int,
    val limit: Int
)

@JsonClass(generateAdapter = true)
data class SavedRepoSummary(
    val fullName: String,
    val name: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val htmlUrl: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)

fun GitHubRepo.toSavedSummary(): SavedRepoSummary = SavedRepoSummary(
    fullName = fullName,
    name = name,
    ownerLogin = owner.login,
    ownerAvatarUrl = owner.avatarUrl,
    description = description,
    language = language,
    stars = stargazersCount,
    forks = forksCount,
    htmlUrl = htmlUrl
)
