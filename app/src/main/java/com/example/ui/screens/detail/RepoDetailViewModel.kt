package com.example.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GitHubIssue
import com.example.data.model.GitHubRelease
import com.example.data.model.GitHubRepo
import com.example.data.model.IssueComment
import com.example.data.model.RepoContentItem
import com.example.data.model.toSavedSummary
import com.example.data.repository.ApiResult
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RepoDetailTab {
    README,
    RELEASES,
    ISSUES,
    FILES
}

data class FilePreviewState(
    val fileName: String,
    val filePath: String,
    val content: String,
    val isJson: Boolean
)

data class RepoDetailUiState(
    val owner: String,
    val repoName: String,
    val activeTab: RepoDetailTab = RepoDetailTab.README,
    val repo: GitHubRepo? = null,
    val isRepoLoading: Boolean = true,
    val repoError: String? = null,

    val readmeMarkdown: String? = null,
    val isReadmeLoading: Boolean = true,
    val readmeError: String? = null,

    val releases: List<GitHubRelease> = emptyList(),
    val isReleasesLoading: Boolean = false,
    val releasesLoaded: Boolean = false,
    val releasesError: String? = null,

    val issues: List<GitHubIssue> = emptyList(),
    val isIssuesLoading: Boolean = false,
    val issuesLoaded: Boolean = false,
    val issuesError: String? = null,
    val expandedIssueNumber: Int? = null,
    val issueComments: Map<Int, List<IssueComment>> = emptyMap(),
    val loadingCommentsFor: Set<Int> = emptySet(),

    val currentPath: String = "",
    val directoryItems: List<RepoContentItem> = emptyList(),
    val isFilesLoading: Boolean = false,
    val filesLoaded: Boolean = false,
    val filesError: String? = null,
    val filePreview: FilePreviewState? = null,
    val isPreviewLoading: Boolean = false
)

class RepoDetailViewModel(
    private val owner: String,
    private val repoName: String,
    private val repository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoDetailUiState(owner = owner, repoName = repoName))
    val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

    init {
        loadRepoAndReadme()
    }

    fun selectTab(tab: RepoDetailTab) {
        _uiState.update { it.copy(activeTab = tab) }
        when (tab) {
            RepoDetailTab.README -> {
                if (_uiState.value.readmeMarkdown == null && !_uiState.value.isReadmeLoading) {
                    loadReadme()
                }
            }
            RepoDetailTab.RELEASES -> {
                if (!_uiState.value.releasesLoaded && !_uiState.value.isReleasesLoading) {
                    loadReleases()
                }
            }
            RepoDetailTab.ISSUES -> {
                if (!_uiState.value.issuesLoaded && !_uiState.value.isIssuesLoading) {
                    loadIssues()
                }
            }
            RepoDetailTab.FILES -> {
                if (!_uiState.value.filesLoaded && !_uiState.value.isFilesLoading) {
                    loadDirectory("")
                }
            }
        }
    }

    fun refreshAll() {
        loadRepoAndReadme(forceRefresh = true)
        when (_uiState.value.activeTab) {
            RepoDetailTab.RELEASES -> loadReleases()
            RepoDetailTab.ISSUES -> loadIssues()
            RepoDetailTab.FILES -> loadDirectory(_uiState.value.currentPath)
            RepoDetailTab.README -> Unit
        }
    }

    fun toggleBookmark() {
        val currentRepo = _uiState.value.repo ?: return
        viewModelScope.launch {
            repository.toggleSavedRepo(currentRepo.toSavedSummary())
        }
    }

    fun toggleIssueComments(issueNumber: Int) {
        val currentlyExpanded = _uiState.value.expandedIssueNumber
        if (currentlyExpanded == issueNumber) {
            _uiState.update { it.copy(expandedIssueNumber = null) }
            return
        }
        _uiState.update { it.copy(expandedIssueNumber = issueNumber) }
        if (!_uiState.value.issueComments.containsKey(issueNumber)) {
            viewModelScope.launch {
                _uiState.update { it.copy(loadingCommentsFor = it.loadingCommentsFor + issueNumber) }
                when (val result = repository.getIssueComments(owner, repoName, issueNumber)) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(
                            issueComments = it.issueComments + (issueNumber to result.data),
                            loadingCommentsFor = it.loadingCommentsFor - issueNumber
                        )
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(loadingCommentsFor = it.loadingCommentsFor - issueNumber)
                    }
                }
            }
        }
    }

    fun openContentItem(item: RepoContentItem) {
        if (item.isDirectory) {
            loadDirectory(item.path)
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isPreviewLoading = true, filesError = null) }
                when (val result = repository.getDecodedFileContent(owner, repoName, item.path)) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            filePreview = FilePreviewState(
                                fileName = item.name,
                                filePath = item.path,
                                content = result.data,
                                isJson = item.name.endsWith(".json", ignoreCase = true)
                            )
                        )
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(isPreviewLoading = false, filesError = result.message)
                    }
                }
            }
        }
    }

    fun navigateUpDirectory() {
        val path = _uiState.value.currentPath
        if (path.isBlank()) return
        val parent = if (path.contains("/")) path.substringBeforeLast("/") else ""
        loadDirectory(parent)
    }

    fun closeFilePreview() {
        _uiState.update { it.copy(filePreview = null) }
    }

    private fun loadRepoAndReadme(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRepoLoading = true, repoError = null) }
            when (val result = repository.getRepository(owner, repoName, forceRefresh)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(repo = result.data, isRepoLoading = false)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isRepoLoading = false, repoError = result.message)
                }
            }
        }
        loadReadme()
    }

    private fun loadReadme() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReadmeLoading = true, readmeError = null) }
            when (val result = repository.getReadmeMarkdown(owner, repoName)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(readmeMarkdown = result.data, isReadmeLoading = false)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReadmeLoading = false, readmeError = result.message)
                }
            }
        }
    }

    fun loadReleases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReleasesLoading = true, releasesError = null) }
            when (val result = repository.getReleases(owner, repoName)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        releases = result.data,
                        isReleasesLoading = false,
                        releasesLoaded = true
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReleasesLoading = false, releasesError = result.message)
                }
            }
        }
    }

    fun loadIssues() {
        viewModelScope.launch {
            _uiState.update { it.copy(isIssuesLoading = true, issuesError = null) }
            when (val result = repository.getIssues(owner, repoName)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        issues = result.data,
                        isIssuesLoading = false,
                        issuesLoaded = true
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isIssuesLoading = false, issuesError = result.message)
                }
            }
        }
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFilesLoading = true, filesError = null, currentPath = path) }
            when (val result = repository.getDirectoryContents(owner, repoName, path)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        directoryItems = result.data,
                        isFilesLoading = false,
                        filesLoaded = true
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isFilesLoading = false, filesError = result.message)
                }
            }
        }
    }

    companion object {
        fun factory(
            owner: String,
            repoName: String,
            repository: GitHubRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RepoDetailViewModel(owner, repoName, repository) as T
        }
    }
}
