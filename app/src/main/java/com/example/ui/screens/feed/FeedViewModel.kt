package com.example.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GitHubEvent
import com.example.data.model.GitHubRepo
import com.example.data.model.toSavedSummary
import com.example.data.repository.ApiResult
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FeedMode {
    TRENDING,
    LIVE_EVENTS,
    FOLLOWING
}

data class FeedTopic(
    val id: String,
    val label: String,
    val query: String
)

data class FeedUiState(
    val mode: FeedMode = FeedMode.TRENDING,
    val selectedTopic: FeedTopic = DEFAULT_TOPICS.first(),
    val repos: List<GitHubRepo> = emptyList(),
    val liveEvents: List<GitHubEvent> = emptyList(),
    val followingEvents: List<GitHubEvent> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    companion object {
        val DEFAULT_TOPICS = listOf(
            FeedTopic("social", "Mạng xã hội", "topic:social-network stars:>500"),
            FeedTopic("web", "Web & UI", "topic:web-app stars:>2000"),
            FeedTopic("android", "Android", "topic:jetpack-compose stars:>500"),
            FeedTopic("ai", "AI & LLM", "topic:ai stars:>3000"),
            FeedTopic("kotlin", "Kotlin", "language:kotlin stars:>2000"),
            FeedTopic("ts", "TypeScript", "language:typescript stars:>5000")
        )
    }
}

class FeedViewModel(
    private val repository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var latestFollowedUsers: List<String> = emptyList()

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                latestFollowedUsers = prefs.followedUsers
            }
        }
        loadCurrentFeed(forceRefresh = false)
    }

    fun selectMode(mode: FeedMode) {
        if (_uiState.value.mode == mode && _uiState.value.errorMessage == null) return
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
        loadCurrentFeed(forceRefresh = false)
    }

    fun selectTopic(topic: FeedTopic) {
        if (_uiState.value.selectedTopic.id == topic.id && _uiState.value.repos.isNotEmpty()) return
        _uiState.update { it.copy(selectedTopic = topic, errorMessage = null) }
        if (_uiState.value.mode == FeedMode.TRENDING) {
            loadTrending(topic, forceRefresh = false)
        }
    }

    fun refresh() {
        loadCurrentFeed(forceRefresh = true)
    }

    fun toggleBookmark(repo: GitHubRepo) {
        viewModelScope.launch {
            repository.toggleSavedRepo(repo.toSavedSummary())
        }
    }

    fun toggleFollow(login: String) {
        viewModelScope.launch {
            repository.toggleFollowUser(login)
        }
    }

    private fun loadCurrentFeed(forceRefresh: Boolean) {
        when (_uiState.value.mode) {
            FeedMode.TRENDING -> loadTrending(_uiState.value.selectedTopic, forceRefresh)
            FeedMode.LIVE_EVENTS -> loadPublicEvents()
            FeedMode.FOLLOWING -> loadFollowingEvents()
        }
    }

    private fun loadTrending(topic: FeedTopic, forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getTrendingRepos(topic.query, forceRefresh)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(repos = result.data, isLoading = false, errorMessage = null)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun loadPublicEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getPublicEvents()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(liveEvents = result.data, isLoading = false, errorMessage = null)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun loadFollowingEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val targets = latestFollowedUsers.ifEmpty {
                listOf("antfu", "shadcn", "JakeWharton", "tiangolo")
            }
            when (val result = repository.getFollowingEvents(targets)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(followingEvents = result.data, isLoading = false, errorMessage = null)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun factory(repository: GitHubRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FeedViewModel(repository) as T
            }
    }
}
