package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.auth.signOutTwin
import com.example.data.local.ThemePreferences
import com.example.data.model.UiState
import com.example.data.remote.CloudinaryUploader
import com.example.data.repository.TwinCommunityRepository
import com.example.data.repository.TwinRepository
import com.example.ui.components.TwinBottomBar
import com.example.ui.components.TwinGlobalSearchDialog
import com.example.ui.components.TwinNotificationsDialog
import com.example.ui.components.TwinRightDrawerOverlay
import com.example.ui.components.TwinRightSidebarContent
import com.example.ui.components.TwinRoute
import com.example.ui.components.TwinTopHeader
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.CreateStoryDialog
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsVideoScreen
import com.example.ui.screens.SavedPostsScreen
import com.example.ui.screens.TwinGuestShell
import com.example.ui.theme.TwinTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun TwinApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember(context) { ThemePreferences(context.applicationContext) }
    val darkMode by themePreferences.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)

    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentAuthUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentAuthUser?.uid) {
        if (currentAuthUser != null) {
            isGuestMode = false
        }
    }

    TwinTheme(darkTheme = darkMode) {
        val user = currentAuthUser
        when {
            user != null -> {
                key(user.uid) {
                    TwinAuthenticatedShell(
                        currentUserId = user.uid,
                        darkMode = darkMode,
                        onSetDarkMode = { enabled ->
                            scope.launch { themePreferences.setDarkMode(enabled) }
                        }
                    )
                }
            }

            isGuestMode -> {
                TwinGuestShell(
                    darkMode = darkMode,
                    onSetDarkMode = { enabled ->
                        scope.launch { themePreferences.setDarkMode(enabled) }
                    },
                    onRequestSignIn = { isGuestMode = false }
                )
            }

            else -> {
                AuthScreen(
                    onAuthSuccess = { isGuestMode = false },
                    onContinueAsGuest = { isGuestMode = true }
                )
            }
        }
    }
}

@Composable
private fun TwinAuthenticatedShell(
    currentUserId: String,
    darkMode: Boolean,
    onSetDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val mainRepository = remember(appContext) { TwinRepository(appContext) }
    val communityRepository = remember(appContext) { TwinCommunityRepository(appContext) }
    val uploader = remember(appContext) { CloudinaryUploader(appContext) }

    val viewModel: TwinMainViewModel = viewModel(
        key = currentUserId,
        factory = TwinMainViewModel.factory(
            currentUserId = currentUserId,
            repository = mainRepository,
            communityRepo = communityRepository,
            uploader = uploader
        )
    )

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
    val pagesState by viewModel.pagesState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val isUploadingMedia by viewModel.isUploadingMedia.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    var currentRouteName by rememberSaveable { mutableStateOf(TwinRoute.HOME.name) }
    var selectedChatUid by rememberSaveable { mutableStateOf<String?>(null) }
    var showRightDrawer by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showGlobalCreateStory by remember { mutableStateOf(false) }

    val currentRoute = remember(currentRouteName) { TwinRoute.valueOf(currentRouteName) }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
    val allGroups = (groupsState as? UiState.Success)?.data.orEmpty()
    val allPages = (pagesState as? UiState.Success)?.data.orEmpty()
    val allNotifications = (notificationsState as? UiState.Success)?.data.orEmpty()

    val unreadNotifCount = remember(allNotifications) {
        allNotifications.count { !it.read && !it.isMessageNotification }
    }
    val unreadChatCount = remember(allNotifications) {
        allNotifications.count { !it.read && it.isMessageNotification }
    }

    fun navigateTo(route: TwinRoute) {
        currentRouteName = route.name
        if (route != TwinRoute.MESSAGES) selectedChatUid = null
    }

    fun openChatWith(targetUid: String) {
        selectedChatUid = targetUid
        currentRouteName = TwinRoute.MESSAGES.name
    }

    if (!showRightDrawer && currentRoute != TwinRoute.HOME && !(currentRoute == TwinRoute.MESSAGES && selectedChatUid != null)) {
        BackHandler { navigateTo(TwinRoute.HOME) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TwinTopHeader(
                        currentUser = currentUser,
                        isGuest = false,
                        unreadNotifCount = unreadNotifCount,
                        unreadChatCount = unreadChatCount,
                        onLogoClick = { navigateTo(TwinRoute.HOME) },
                        onSearchClick = { showSearchDialog = true },
                        onChatClick = { navigateTo(TwinRoute.MESSAGES) },
                        onNotificationsClick = { showNotificationsDialog = true },
                        onOpenRightDrawer = { showRightDrawer = true }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        TwinBottomBar(
                            currentRoute = currentRoute,
                            onSelectRoute = ::navigateTo
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Left Navigation Rail on Tablets / Foldables (matching web 3-column layout)
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                        ) {
                            TwinRoute.bottomTabs.forEach { tab ->
                                val selected = tab == currentRoute
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("rail_tab_${tab.key}")
                                )
                            }
                        }
                    }

                    // Main Center Column
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentRoute) {
                            TwinRoute.HOME -> HomeFeedScreen(viewModel = viewModel)

                            TwinRoute.REELS -> ReelsVideoScreen(
                                storiesState = storiesState,
                                postsState = postsState,
                                currentUid = viewModel.currentUserId,
                                onToggleStoryLike = { viewModel.toggleStoryLike(it) },
                                onCreateStory = { showGlobalCreateStory = true },
                                onOpenVideoUrl = { url ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            )

                            TwinRoute.MARKET -> MarketScreen(
                                postsState = postsState,
                                currentUid = viewModel.currentUserId,
                                isGuest = false,
                                isUploading = isUploadingMedia,
                                onCreateProduct = { title, price, cat, cond, loc, desc, uri, onDone ->
                                    viewModel.createMarketProduct(title, price, cat, cond, loc, desc, uri, onDone)
                                },
                                onMessageSeller = ::openChatWith,
                                onDeleteProduct = { viewModel.deletePost(it) },
                                onRequireSignIn = {}
                            )

                            TwinRoute.LEARN -> LearnScreen()

                            TwinRoute.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                darkMode = darkMode,
                                onToggleDarkMode = onSetDarkMode
                            )

                            TwinRoute.FRIENDS -> CommunityScreen(
                                viewModel = viewModel,
                                initialTab = 0,
                                onOpenChat = ::openChatWith
                            )

                            TwinRoute.GROUPS -> CommunityScreen(
                                viewModel = viewModel,
                                initialTab = 1,
                                onOpenChat = ::openChatWith
                            )

                            TwinRoute.PAGES -> CommunityScreen(
                                viewModel = viewModel,
                                initialTab = 2,
                                onOpenChat = ::openChatWith
                            )

                            TwinRoute.MESSAGES -> MessagesScreen(
                                viewModel = viewModel,
                                selectedChatUid = selectedChatUid,
                                onSelectChat = { selectedChatUid = it },
                                onClearChat = { selectedChatUid = null }
                            )

                            TwinRoute.SAVED -> SavedPostsScreen(viewModel = viewModel)

                            TwinRoute.HELP -> HelpScreen()
                        }
                    }

                    // Persistent Right Sidebar on Wide Screens (matching web 3-column right panel)
                    if (isWideScreen) {
                        TwinRightSidebarContent(
                            currentUser = currentUser,
                            isGuest = false,
                            darkMode = darkMode,
                            postsCount = allPosts.size,
                            users = allUsers,
                            groupsCount = allGroups.size,
                            showCloseButton = false,
                            onClose = {},
                            onNavigate = ::navigateTo,
                            onOpenChatWith = ::openChatWith,
                            onToggleDarkMode = { onSetDarkMode(!darkMode) },
                            onPasswordReset = { viewModel.sendPasswordResetForCurrentUser() },
                            onSignOutOrSignIn = {
                                signOutTwin(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onBeforeSignOut = { viewModel.markOfflineBeforeSignOut() }
                                )
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(300.dp)
                        )
                    }
                }
            }

            // Expandable Right Drawer on Compact & Medium screens ("Thanh bên phải mở rộng")
            if (!isWideScreen) {
                TwinRightDrawerOverlay(
                    visible = showRightDrawer,
                    currentUser = currentUser,
                    isGuest = false,
                    darkMode = darkMode,
                    postsCount = allPosts.size,
                    users = allUsers,
                    groupsCount = allGroups.size,
                    onDismiss = { showRightDrawer = false },
                    onNavigate = ::navigateTo,
                    onOpenChatWith = ::openChatWith,
                    onToggleDarkMode = { onSetDarkMode(!darkMode) },
                    onPasswordReset = { viewModel.sendPasswordResetForCurrentUser() },
                    onSignOutOrSignIn = {
                        signOutTwin(
                            credentialManager = credentialManager,
                            scope = scope,
                            onBeforeSignOut = { viewModel.markOfflineBeforeSignOut() }
                        )
                    }
                )
            }
        }
    }

    if (showSearchDialog) {
        TwinGlobalSearchDialog(
            users = allUsers,
            posts = allPosts,
            groups = allGroups,
            pages = allPages,
            onOpenChatWith = ::openChatWith,
            onNavigateRoute = ::navigateTo,
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showNotificationsDialog) {
        TwinNotificationsDialog(
            notificationsState = notificationsState,
            onMarkRead = { viewModel.markNotificationRead(it) },
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onOpenNotification = { notif ->
                showNotificationsDialog = false
                when {
                    notif.isMessageNotification && notif.fromUid.isNotBlank() -> openChatWith(notif.fromUid)
                    notif.isMessageNotification -> navigateTo(TwinRoute.MESSAGES)
                    notif.storyId.isNotBlank() -> navigateTo(TwinRoute.REELS)
                    notif.type == "friend_request" || notif.type == "friend_accept" -> navigateTo(TwinRoute.FRIENDS)
                    else -> navigateTo(TwinRoute.HOME)
                }
            },
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showGlobalCreateStory) {
        CreateStoryDialog(
            isUploading = isUploadingMedia,
            onDismiss = { if (!isUploadingMedia) showGlobalCreateStory = false },
            onPublish = { text, bgId, imageUri, caption ->
                viewModel.createStory(text, bgId, imageUri, caption) {
                    showGlobalCreateStory = false
                }
            }
        )
    }
}
