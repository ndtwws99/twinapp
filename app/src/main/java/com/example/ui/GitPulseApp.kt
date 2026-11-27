package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.UserPreferences
import com.example.data.model.GitHubRepo
import com.example.data.repository.GitHubRepository
import com.example.ui.screens.detail.RepoDetailScreen
import com.example.ui.screens.detail.RepoDetailViewModel
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.explore.ExploreViewModel
import com.example.ui.screens.feed.FeedScreen
import com.example.ui.screens.feed.FeedViewModel
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.saved.SavedScreen
import com.example.ui.screens.user.UserProfileScreen
import com.example.ui.screens.user.UserProfileViewModel
import com.example.ui.theme.GitPulseTheme
import kotlinx.coroutines.launch

const val ROUTE_FEED = "feed"
const val ROUTE_EXPLORE = "explore"
const val ROUTE_SAVED = "saved"
const val ROUTE_PROFILE = "profile"
const val ROUTE_REPO_DETAIL = "repo/{owner}/{name}"
const val ROUTE_USER_PROFILE = "user/{login}"

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(
        route = ROUTE_FEED,
        labelRes = R.string.nav_feed,
        selectedIcon = Icons.Filled.DynamicFeed,
        unselectedIcon = Icons.Outlined.DynamicFeed,
        testTag = "nav_feed"
    ),
    TopLevelDestination(
        route = ROUTE_EXPLORE,
        labelRes = R.string.nav_explore,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_explore"
    ),
    TopLevelDestination(
        route = ROUTE_SAVED,
        labelRes = R.string.nav_saved,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_saved"
    ),
    TopLevelDestination(
        route = ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline,
        testTag = "nav_profile"
    )
)

@Composable
fun GitPulseApp(repository: GitHubRepository) {
    val preferences by repository.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )
    val rateLimit by repository.rateLimit.collectAsStateWithLifecycle()

    LaunchedEffect(preferences.githubToken) {
        repository.updateActiveToken(preferences.githubToken)
    }

    GitPulseTheme(darkTheme = preferences.darkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isTopLevel = currentRoute in setOf(ROUTE_FEED, ROUTE_EXPLORE, ROUTE_SAVED, ROUTE_PROFILE)
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val feedViewModel: FeedViewModel = viewModel(factory = FeedViewModel.factory(repository))
        val exploreViewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.factory(repository))
        val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(repository))

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        fun openRepoDetail(owner: String, repo: String) {
            val safeOwner = Uri.encode(owner)
            val safeRepo = Uri.encode(repo)
            navController.navigate("repo/$safeOwner/$safeRepo")
        }

        fun openUserProfile(login: String) {
            val safeLogin = Uri.encode(login)
            navController.navigate("user/$safeLogin")
        }

        fun shareRepo(repo: GitHubRepo) {
            shareLink(context, repo.fullName, repo.htmlUrl)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (isTopLevel && !useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            TopLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTopLevel && useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            TopLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_FEED,
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 840.dp)
                        ) {
                            composable(ROUTE_FEED) {
                                FeedScreen(
                                    viewModel = feedViewModel,
                                    preferences = preferences,
                                    rateLimit = rateLimit,
                                    onOpenRepo = ::openRepoDetail,
                                    onOpenUser = ::openUserProfile,
                                    onOpenProfileSettings = { navigateTopLevel(ROUTE_PROFILE) },
                                    onShareRepo = ::shareRepo
                                )
                            }

                            composable(ROUTE_EXPLORE) {
                                ExploreScreen(
                                    viewModel = exploreViewModel,
                                    preferences = preferences,
                                    onOpenRepo = ::openRepoDetail,
                                    onOpenUser = ::openUserProfile,
                                    onShareRepo = ::shareRepo
                                )
                            }

                            composable(ROUTE_SAVED) {
                                SavedScreen(
                                    savedRepos = preferences.savedRepos,
                                    onOpenRepo = ::openRepoDetail,
                                    onOpenUser = ::openUserProfile,
                                    onRemoveSaved = { summary ->
                                        scope.launch { repository.toggleSavedRepo(summary) }
                                    },
                                    onShareUrl = { title, url -> shareLink(context, title, url) }
                                )
                            }

                            composable(ROUTE_PROFILE) {
                                ProfileScreen(
                                    viewModel = profileViewModel,
                                    preferences = preferences,
                                    rateLimit = rateLimit,
                                    onOpenRepo = ::openRepoDetail,
                                    onOpenUser = ::openUserProfile
                                )
                            }

                            composable(
                                route = ROUTE_REPO_DETAIL,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("name") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner").orEmpty()
                                val name = backStackEntry.arguments?.getString("name").orEmpty()
                                val detailVm: RepoDetailViewModel = viewModel(
                                    key = "repo_${owner}_$name",
                                    factory = RepoDetailViewModel.factory(owner, name, repository)
                                )
                                RepoDetailScreen(
                                    viewModel = detailVm,
                                    preferences = preferences,
                                    onBack = { navController.popBackStack() },
                                    onOpenUser = ::openUserProfile,
                                    onOpenUrl = { url -> openExternalUrl(context, url) },
                                    onShareRepo = ::shareRepo
                                )
                            }

                            composable(
                                route = ROUTE_USER_PROFILE,
                                arguments = listOf(
                                    navArgument("login") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val login = backStackEntry.arguments?.getString("login").orEmpty()
                                val userVm: UserProfileViewModel = viewModel(
                                    key = "user_$login",
                                    factory = UserProfileViewModel.factory(login, repository)
                                )
                                UserProfileScreen(
                                    viewModel = userVm,
                                    preferences = preferences,
                                    onBack = { navController.popBackStack() },
                                    onOpenRepo = ::openRepoDetail,
                                    onOpenUser = ::openUserProfile,
                                    onOpenUrl = { url -> openExternalUrl(context, url) },
                                    onShareRepo = ::shareRepo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

private fun shareLink(context: Context, title: String, url: String) {
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title — $url")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ qua"))
    }
}
