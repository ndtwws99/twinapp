package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.repository.ApiResult
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val usernameInput: String = "",
    val tokenInput: String = "",
    val connectedUser: GitHubUser? = null,
    val userRepos: List<GitHubRepo> = emptyList(),
    val isLoadingUser: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class ProfileViewModel(
    private val repository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = repository.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    usernameInput = prefs.connectedUsername,
                    tokenInput = prefs.githubToken
                )
            }
            if (prefs.connectedUsername.isNotBlank()) {
                fetchConnectedUser(prefs.connectedUsername)
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(usernameInput = value, statusMessage = null) }
    }

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(tokenInput = value, statusMessage = null) }
    }

    fun saveAndConnect() {
        val username = _uiState.value.usernameInput.trim().removePrefix("@")
        val token = _uiState.value.tokenInput.trim()
        viewModelScope.launch {
            repository.updateConnection(username, token)
            if (username.isNotBlank()) {
                fetchConnectedUser(username)
            } else {
                _uiState.update {
                    it.copy(
                        connectedUser = null,
                        userRepos = emptyList(),
                        statusMessage = if (token.isNotBlank()) {
                            "Đã lưu GitHub Token — giới hạn API đã được nâng cấp."
                        } else {
                            "Đã cập nhật cấu hình nguồn GitHub."
                        },
                        isError = false
                    )
                }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun unfollowUser(login: String) {
        viewModelScope.launch {
            repository.toggleFollowUser(login)
        }
    }

    private fun fetchConnectedUser(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUser = true, statusMessage = null, isError = false) }
            when (val userRes = repository.getUserProfile(username)) {
                is ApiResult.Success -> {
                    val reposRes = repository.getUserRepos(username)
                    val repos = (reposRes as? ApiResult.Success)?.data.orEmpty()
                    _uiState.update {
                        it.copy(
                            connectedUser = userRes.data,
                            userRepos = repos.take(8),
                            isLoadingUser = false,
                            statusMessage = "Đã đồng bộ hồ sơ @${userRes.data.login} từ GitHub API.",
                            isError = false
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isLoadingUser = false,
                        statusMessage = userRes.message,
                        isError = true
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: GitHubRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProfileViewModel(repository) as T
            }
    }
}
