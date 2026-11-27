package com.example.ui.screens.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GitHubEvent
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

enum class UserProfileTab {
    REPOS,
    ACTIVITY
}

data class UserProfileUiState(
    val username: String,
    val activeTab: UserProfileTab = UserProfileTab.REPOS,
    val user: GitHubUser? = null,
    val isUserLoading: Boolean = true,
    val userError: String? = null,
    val repos: List<GitHubRepo> = emptyList(),
    val isReposLoading: Boolean = true,
    val events: List<GitHubEvent> = emptyList(),
    val isEventsLoading: Boolean = false,
    val eventsLoaded: Boolean = false
)

class UserProfileViewModel(
    private val username: String,
    private val repository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState(username = username))
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserAndRepos()
    }

    fun selectTab(tab: UserProfileTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == UserProfileTab.ACTIVITY && !_uiState.value.eventsLoaded && !_uiState.value.isEventsLoading) {
            loadEvents()
        }
    }

    fun refresh() {
        loadUserAndRepos()
        if (_uiState.value.activeTab == UserProfileTab.ACTIVITY) {
            loadEvents()
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            repository.toggleFollowUser(username)
        }
    }

    fun toggleBookmark(repo: GitHubRepo) {
        viewModelScope.launch {
            repository.toggleSavedRepo(repo.toSavedSummary())
        }
    }

    private fun loadUserAndRepos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserLoading = true, userError = null) }
            when (val res = repository.getUserProfile(username)) {
                is ApiResult.Success -> _uiState.update { it.copy(user = res.data, isUserLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isUserLoading = false, userError = res.message) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isReposLoading = true) }
            when (val res = repository.getUserRepos(username)) {
                is ApiResult.Success -> _uiState.update { it.copy(repos = res.data, isReposLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isReposLoading = false) }
            }
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isEventsLoading = true) }
            when (val res = repository.getUserEvents(username)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(events = res.data, isEventsLoading = false, eventsLoaded = true)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isEventsLoading = false, eventsLoaded = true)
                }
            }
        }
    }

    companion object {
        fun factory(
            username: String,
            repository: GitHubRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UserProfileViewModel(username, repository) as T
        }
    }
}
