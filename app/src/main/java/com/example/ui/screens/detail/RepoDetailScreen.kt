package com.example.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.UserPreferences
import com.example.data.model.GitHubRepo
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LanguageDotBadge
import com.example.ui.components.MarkdownRenderer
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.MonoHandleStyle
import com.example.ui.theme.PulseAmber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepoDetailScreen(
    viewModel: RepoDetailViewModel,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onOpenUser: (login: String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onShareRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fullName = "${state.owner}/${state.repoName}"
    val isBookmarked = preferences.savedRepos.any { it.fullName.equals(fullName, ignoreCase = true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.repoName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${state.owner}",
                            style = MonoHandleStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("repo_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleBookmark() },
                        modifier = Modifier.testTag("repo_detail_bookmark")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.repo_bookmark),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    state.repo?.let { repo ->
                        IconButton(
                            onClick = { onShareRepo(repo) },
                            modifier = Modifier.testTag("repo_detail_share")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.repo_share)
                            )
                        }
                        IconButton(
                            onClick = { onOpenUrl(repo.htmlUrl) },
                            modifier = Modifier.testTag("repo_detail_open_browser")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = stringResource(R.string.repo_open_github)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Repo Hero Card
            item(key = "repo_hero") {
                RepoHeroHeaderCard(
                    state = state,
                    onOpenOwner = { onOpenUser(state.owner) },
                    onRetry = { viewModel.refreshAll() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 4 Inner Tabs: README, Releases, Thảo luận, Tệp & JSON
            item(key = "repo_tabs") {
                val tabs = listOf(
                    Triple(RepoDetailTab.README, stringResource(R.string.repo_tab_readme), Icons.Outlined.Description),
                    Triple(RepoDetailTab.RELEASES, stringResource(R.string.repo_tab_releases), Icons.Outlined.NewReleases),
                    Triple(RepoDetailTab.ISSUES, stringResource(R.string.repo_tab_issues), Icons.Outlined.ChatBubbleOutline),
                    Triple(RepoDetailTab.FILES, stringResource(R.string.repo_tab_files), Icons.Outlined.FolderOpen)
                )
                ScrollableTabRow(
                    selectedTabIndex = state.activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 12.dp
                ) {
                    tabs.forEach { (tab, title, icon) ->
                        Tab(
                            selected = state.activeTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(title, style = MaterialTheme.typography.labelLarge)
                                }
                            },
                            modifier = Modifier.testTag("repo_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            when (state.activeTab) {
                RepoDetailTab.README -> {
                    item(key = "readme_content") {
                        when {
                            state.isReadmeLoading -> LoadingBox()
                            state.readmeError != null -> EmptyStateView(
                                icon = Icons.Outlined.Description,
                                title = "Không có README",
                                subtitle = stringResource(R.string.repo_no_readme)
                            )
                            state.readmeMarkdown.isNullOrBlank() -> EmptyStateView(
                                icon = Icons.Outlined.Description,
                                title = "README trống",
                                subtitle = stringResource(R.string.repo_no_readme)
                            )
                            else -> Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                MarkdownRenderer(
                                    markdown = state.readmeMarkdown.orEmpty(),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                RepoDetailTab.RELEASES -> {
                    when {
                        state.isReleasesLoading -> item(key = "rel_load") { LoadingBox() }
                        state.releasesError != null -> item(key = "rel_err") {
                            ErrorStateView(
                                message = state.releasesError.orEmpty(),
                                onRetry = { viewModel.loadReleases() }
                            )
                        }
                        state.releases.isEmpty() -> item(key = "rel_empty") {
                            EmptyStateView(
                                icon = Icons.Outlined.NewReleases,
                                title = "Chưa có Releases",
                                subtitle = stringResource(R.string.repo_no_releases)
                            )
                        }
                        else -> items(state.releases, key = { it.id }) { release ->
                            ReleasePostCard(
                                release = release,
                                onOpenUrl = onOpenUrl,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                RepoDetailTab.ISSUES -> {
                    when {
                        state.isIssuesLoading -> item(key = "iss_load") { LoadingBox() }
                        state.issuesError != null -> item(key = "iss_err") {
                            ErrorStateView(
                                message = state.issuesError.orEmpty(),
                                onRetry = { viewModel.loadIssues() }
                            )
                        }
                        state.issues.isEmpty() -> item(key = "iss_empty") {
                            EmptyStateView(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                title = "Chưa có thảo luận",
                                subtitle = stringResource(R.string.repo_no_issues)
                            )
                        }
                        else -> items(state.issues, key = { it.id }) { issue ->
                            IssueThreadCard(
                                issue = issue,
                                isExpanded = state.expandedIssueNumber == issue.number,
                                comments = state.issueComments[issue.number].orEmpty(),
                                isLoadingComments = issue.number in state.loadingCommentsFor,
                                onToggleComments = { viewModel.toggleIssueComments(issue.number) },
                                onOpenUser = onOpenUser,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                RepoDetailTab.FILES -> {
                    item(key = "files_explorer") {
                        RepoFilesExplorerSection(
                            state = state,
                            onNavigateUp = { viewModel.navigateUpDirectory() },
                            onOpenItem = { viewModel.openContentItem(it) },
                            onRetry = { viewModel.loadDirectory(state.currentPath) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // In-App JSON & Source Code Preview Modal
        state.filePreview?.let { preview ->
            FileContentPreviewDialog(
                preview = preview,
                onDismiss = { viewModel.closeFilePreview() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepoHeroHeaderCard(
    state: RepoDetailUiState,
    onOpenOwner: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        when {
            state.isRepoLoading && state.repo == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.repoError != null && state.repo == null -> {
                ErrorStateView(message = state.repoError, onRetry = onRetry)
            }

            state.repo != null -> {
                val repo = state.repo
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenOwner)
                    ) {
                        AsyncImage(
                            model = repo.owner.avatarUrl,
                            contentDescription = "Avatar @${repo.owner.login}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = repo.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Nhánh mặc định: ${repo.defaultBranch}" +
                                    (repo.license?.spdxId?.let { " • $it" } ?: ""),
                                style = MonoHandleStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!repo.description.isNullOrBlank()) {
                        Text(
                            text = repo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            icon = Icons.Filled.Star,
                            value = formatCompactCount(repo.stargazersCount),
                            label = "Stars",
                            tint = PulseAmber,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Outlined.CallSplit,
                            value = formatCompactCount(repo.forksCount),
                            label = "Forks",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            value = formatCompactCount(repo.openIssuesCount),
                            label = "Issues",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Outlined.RemoveRedEye,
                            value = formatCompactCount(repo.watchersCount),
                            label = "Watch",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!repo.language.isNullOrBlank() || repo.topics.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!repo.language.isNullOrBlank()) {
                                LanguageDotBadge(language = repo.language)
                            }
                            repo.topics.take(6).forEach { topic ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "#$topic",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MonoHandleStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
