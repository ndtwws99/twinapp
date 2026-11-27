package com.example.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.toSavedSummary
import com.example.data.repository.ApiResult
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTarget {
    REPOSITORIES,
    USERS
}

data class ExploreUiState(
    val query: String = "social-network",
    val target: SearchTarget = SearchTarget.REPOSITORIES,
    val repos: List<GitHubRepo> = emptyList(),
    val users: List<GitHubUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
) {
    companion object {
        val QUICK_TOPICS = listOf(
            "social-network",
            "mastodon",
            "bluesky",
            "nextjs",
            "jetpack-compose",
            "react",
            "rust",
            "kotlin"
        )
    }
}

class ExploreViewModel(
    private val repository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        submitSearch()
    }

    fun updateQuery(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
    }

    fun selectTarget(target: SearchTarget) {
        if (_uiState.value.target == target) return
        _uiState.update { it.copy(target = target, errorMessage = null) }
        submitSearch()
    }

    fun applyQuickTopic(topic: String) {
        _uiState.update { it.copy(query = topic, target = SearchTarget.REPOSITORIES) }
        submitSearch()
    }

    fun submitSearch() {
        val q = _uiState.value.query.trim()
        if (q.isBlank()) return
        val target = _uiState.value.target
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasSearched = true) }
            when (target) {
                SearchTarget.REPOSITORIES -> {
                    when (val result = repository.searchRepositories(q)) {
                        is ApiResult.Success -> _uiState.update {
                            it.copy(repos = result.data, isLoading = false)
                        }
                        is ApiResult.Error -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
                SearchTarget.USERS -> {
                    when (val result = repository.searchUsers(q)) {
                        is ApiResult.Success -> _uiState.update {
                            it.copy(users = result.data, isLoading = false)
                        }
                        is ApiResult.Error -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
            }
        }
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

    companion object {
        fun factory(repository: GitHubRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExploreViewModel(repository) as T
            }
    }
}
