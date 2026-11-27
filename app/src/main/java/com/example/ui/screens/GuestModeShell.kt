package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.TwinComment
import com.example.data.model.TwinPost
import com.example.data.model.TwinStory
import com.example.data.model.UiState
import com.example.ui.components.CommentsDialog
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.TwinAvatar
import com.example.ui.components.TwinBottomBar
import com.example.ui.components.TwinGlobalSearchDialog
import com.example.ui.components.TwinPostCard
import com.example.ui.components.TwinRightDrawerOverlay
import com.example.ui.components.TwinRightSidebarContent
import com.example.ui.components.TwinRoute
import com.example.ui.components.TwinTopHeader
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.UUID

private const val GUEST_UID = "guest_local_session"
private const val GUEST_NAME = "Khách Twin"
private val GUEST_FEELINGS = listOf("Vui vẻ", "Hào hứng", "Đang code", "Hạnh phúc", "Thư giãn")
private val GUEST_FILTERS = listOf("Tất cả", "Ảnh", "Tệp đính kèm")

/**
 * Fully local, zero-Firestore Guest Mode shell sharing the exact Twin web chrome:
 * Unified Top Header + 5-tab Bottom Bar + Expandable Right Sidebar Drawer.
 */
@Composable
fun TwinGuestShell(
    darkMode: Boolean,
    onSetDarkMode: (Boolean) -> Unit,
    onRequestSignIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentRouteName by rememberSaveable { mutableStateOf(TwinRoute.HOME.name) }
    val currentRoute = remember(currentRouteName) { TwinRoute.valueOf(currentRouteName) }
    var showRightDrawer by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    var localPosts by remember { mutableStateOf<List<TwinPost>>(emptyList()) }
    var localStories by remember { mutableStateOf<List<TwinStory>>(emptyList()) }
    var showCreatePost by remember { mutableStateOf(false) }
    var showCreateStory by remember { mutableStateOf(false) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var activeStoryId by remember { mutableStateOf<String?>(null) }

    if (!showRightDrawer && currentRoute != TwinRoute.HOME) {
        BackHandler { currentRouteName = TwinRoute.HOME.name }
    }

    val activeCommentPost = remember(localPosts, activeCommentPostId) {
        localPosts.firstOrNull { it.id == activeCommentPostId }
    }
    val activeStory = remember(localStories, activeStoryId) {
        localStories.firstOrNull { it.id == activeStoryId }
    }

    fun showToast(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun toggleLocalReaction(postId: String, key: String) {
        localPosts = localPosts.map { post ->
            if (post.id != postId) post else {
                val updated = post.reactions.toMutableMap()
                if (updated[GUEST_UID] == key) updated.remove(GUEST_UID) else updated[GUEST_UID] = key
                post.copy(reactions = updated)
            }
        }
    }

    fun toggleLocalSave(postId: String) {
        localPosts = localPosts.map { post ->
            if (post.id != postId) post else {
                val updated = if (GUEST_UID in post.savedBy) post.savedBy - GUEST_UID else post.savedBy + GUEST_UID
                post.copy(savedBy = updated)
            }
        }
    }

    fun addLocalComment(postId: String, text: String) {
        val comment = TwinComment(
            commentId = UUID.randomUUID().toString(),
            authorUid = GUEST_UID,
            authorName = GUEST_NAME,
            text = text,
            createdAtMillis = System.currentTimeMillis()
        )
        localPosts = localPosts.map { post ->
            if (post.id != postId) post else post.copy(comments = post.comments + comment.toMap())
        }
    }

    fun shareLocalPost(post: TwinPost) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$GUEST_NAME trên Twin:\n${post.content}")
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ bài viết Twin"))
        localPosts = localPosts.map {
            if (it.id == post.id) it.copy(sharesCount = it.sharesCount + 1) else it
        }
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
                        currentUser = null,
                        isGuest = true,
                        unreadNotifCount = 0,
                        unreadChatCount = 0,
                        onLogoClick = { currentRouteName = TwinRoute.HOME.name },
                        onSearchClick = { showSearchDialog = true },
                        onChatClick = { currentRouteName = TwinRoute.MESSAGES.name },
                        onNotificationsClick = {},
                        onOpenRightDrawer = { showRightDrawer = true }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        TwinBottomBar(
                            currentRoute = currentRoute,
                            onSelectRoute = { currentRouteName = it.name }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
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
                                    onClick = { currentRouteName = tab.name },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("guest_rail_${tab.key}")
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Compact Guest Banner
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.guest_banner_label),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onRequestSignIn,
                                    modifier = Modifier.testTag("guest_banner_sign_in")
                                ) {
                                    Text(
                                        text = stringResource(R.string.guest_sign_in_cta),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (currentRoute) {
                                TwinRoute.HOME -> GuestHomeContent(
                                    posts = localPosts,
                                    stories = localStories,
                                    onCreatePost = { showCreatePost = true },
                                    onCreateStory = { showCreateStory = true },
                                    onOpenStory = { activeStoryId = it.id },
                                    onReact = ::toggleLocalReaction,
                                    onOpenComments = { activeCommentPostId = it },
                                    onToggleSave = ::toggleLocalSave,
                                    onShare = ::shareLocalPost,
                                    onDelete = { id ->
                                        localPosts = localPosts.filterNot { it.id == id }
                                        showToast("Đã xóa bài viết nháp.")
                                    },
                                    onRequestSignIn = onRequestSignIn
                                )

                                TwinRoute.REELS -> ReelsVideoScreen(
                                    storiesState = UiState.Success(localStories),
                                    postsState = UiState.Success(localPosts),
                                    currentUid = GUEST_UID,
                                    onToggleStoryLike = { story ->
                                        localStories = localStories.map { item ->
                                            if (item.id != story.id) item else {
                                                val updated = if (GUEST_UID in item.likes) item.likes - GUEST_UID else item.likes + GUEST_UID
                                                item.copy(likes = updated)
                                            }
                                        }
                                    },
                                    onCreateStory = { showCreateStory = true },
                                    onOpenVideoUrl = { url ->
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    }
                                )

                                TwinRoute.MARKET -> MarketScreen(
                                    postsState = UiState.Success(localPosts),
                                    currentUid = GUEST_UID,
                                    isGuest = true,
                                    isUploading = false,
                                    onCreateProduct = { _, _, _, _, _, _, _, _ -> onRequestSignIn() },
                                    onMessageSeller = { onRequestSignIn() },
                                    onDeleteProduct = { id -> localPosts = localPosts.filterNot { it.id == id } },
                                    onRequireSignIn = onRequestSignIn
                                )

                                TwinRoute.LEARN -> LearnScreen()

                                TwinRoute.PROFILE -> GuestProfileContent(
                                    postCount = localPosts.size,
                                    savedCount = localPosts.count { GUEST_UID in it.savedBy },
                                    darkMode = darkMode,
                                    onToggleDarkMode = onSetDarkMode,
                                    onRequestSignIn = onRequestSignIn
                                )

                                TwinRoute.SAVED -> GuestSavedContent(
                                    savedPosts = localPosts.filter { GUEST_UID in it.savedBy },
                                    onReact = ::toggleLocalReaction,
                                    onOpenComments = { activeCommentPostId = it },
                                    onToggleSave = ::toggleLocalSave,
                                    onShare = ::shareLocalPost,
                                    onDelete = { id -> localPosts = localPosts.filterNot { it.id == id } }
                                )

                                TwinRoute.HELP -> HelpScreen()

                                TwinRoute.FRIENDS, TwinRoute.GROUPS, TwinRoute.PAGES -> GuestSignInRequiredScreen(
                                    title = "Cộng đồng Twin (${currentRoute.label})",
                                    subtitle = "Đăng nhập bằng tài khoản Twin hoặc Google để kết bạn, tham gia Nhóm và theo dõi Trang đồng bộ trực tiếp với web Twin.",
                                    onSignIn = onRequestSignIn
                                )

                                TwinRoute.MESSAGES -> GuestSignInRequiredScreen(
                                    title = "Twin Chat thời gian thực",
                                    subtitle = "Đăng nhập bằng tài khoản Twin hoặc Google để trò chuyện riêng tư, gửi ảnh và tệp trực tiếp với bạn bè trên Twin.",
                                    onSignIn = onRequestSignIn
                                )
                            }
                        }
                    }

                    if (isWideScreen) {
                        TwinRightSidebarContent(
                            currentUser = null,
                            isGuest = true,
                            darkMode = darkMode,
                            postsCount = localPosts.size,
                            users = emptyList(),
                            groupsCount = 0,
                            showCloseButton = false,
                            onClose = {},
                            onNavigate = { currentRouteName = it.name },
                            onOpenChatWith = { onRequestSignIn() },
                            onToggleDarkMode = { onSetDarkMode(!darkMode) },
                            onPasswordReset = {},
                            onSignOutOrSignIn = onRequestSignIn,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(300.dp)
                        )
                    }
                }
            }

            // Expandable Right Drawer on Compact & Medium screens
            if (!isWideScreen) {
                TwinRightDrawerOverlay(
                    visible = showRightDrawer,
                    currentUser = null,
                    isGuest = true,
                    darkMode = darkMode,
                    postsCount = localPosts.size,
                    users = emptyList(),
                    groupsCount = 0,
                    onDismiss = { showRightDrawer = false },
                    onNavigate = { currentRouteName = it.name },
                    onOpenChatWith = { onRequestSignIn() },
                    onToggleDarkMode = { onSetDarkMode(!darkMode) },
                    onPasswordReset = {},
                    onSignOutOrSignIn = onRequestSignIn
                )
            }
        }
    }

    if (showSearchDialog) {
        TwinGlobalSearchDialog(
            users = emptyList(),
            posts = localPosts,
            groups = emptyList(),
            pages = emptyList(),
            onOpenChatWith = { onRequestSignIn() },
            onNavigateRoute = { currentRouteName = it.name },
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showCreatePost) {
        GuestCreatePostDialog(
            onDismiss = { showCreatePost = false },
            onPublish = { content, feeling, imageUri, fileUri, fileName ->
                val newPost = TwinPost(
                    id = UUID.randomUUID().toString(),
                    authorUid = GUEST_UID,
                    authorName = GUEST_NAME,
                    content = content.trim(),
                    feeling = feeling,
                    privacy = "public",
                    imageUrl = imageUri?.toString().orEmpty(),
                    fileUrl = fileUri?.toString().orEmpty(),
                    fileName = fileName,
                    createdAt = Timestamp.now()
                )
                localPosts = listOf(newPost) + localPosts
                showCreatePost = false
                showToast("Đã tạo bài viết trong chế độ khách (chỉ lưu tạm trên máy).")
            }
        )
    }

    if (showCreateStory) {
        CreateStoryDialog(
            isUploading = false,
            onDismiss = { showCreateStory = false },
            onPublish = { text, bgId, imageUri, caption ->
                val newStory = TwinStory(
                    id = UUID.randomUUID().toString(),
                    type = if (imageUri != null) "image" else "text",
                    authorUid = GUEST_UID,
                    authorName = GUEST_NAME,
                    text = text.trim(),
                    bgId = bgId,
                    imageUrl = imageUri?.toString().orEmpty(),
                    caption = caption.trim(),
                    privacy = "public",
                    createdAt = Timestamp.now()
                )
                localStories = listOf(newStory) + localStories
                showCreateStory = false
                showToast("Đã tạo Tin 24h trong chế độ khách.")
            }
        )
    }

    if (activeCommentPost != null) {
        CommentsDialog(
            post = activeCommentPost,
            onDismiss = { activeCommentPostId = null },
            onSendComment = { text -> addLocalComment(activeCommentPost.id, text) }
        )
    }

    if (activeStory != null) {
        StoryViewerDialog(
            story = activeStory,
            currentUid = GUEST_UID,
            onToggleLike = {
                localStories = localStories.map { story ->
                    if (story.id != activeStory.id) story else {
                        val updated = if (GUEST_UID in story.likes) story.likes - GUEST_UID else story.likes + GUEST_UID
                        story.copy(likes = updated)
                    }
                }
            },
            onDismiss = { activeStoryId = null }
        )
    }
}

@Composable
private fun GuestHomeContent(
    posts: List<TwinPost>,
    stories: List<TwinStory>,
    onCreatePost: () -> Unit,
    onCreateStory: () -> Unit,
    onOpenStory: (TwinStory) -> Unit,
    onReact: (String, String) -> Unit,
    onOpenComments: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onShare: (TwinPost) -> Unit,
    onDelete: (String) -> Unit,
    onRequestSignIn: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    val filteredPosts = remember(posts, selectedFilter) {
        when (selectedFilter) {
            "Ảnh" -> posts.filter { it.primaryImageUrl.isNotBlank() }
            "Tệp đính kèm" -> posts.filter { it.fileUrl.isNotBlank() }
            else -> posts
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "guest_composer") {
                WebStyleComposerCard(
                    avatarUrl = "",
                    userName = GUEST_NAME,
                    onOpenComposer = onCreatePost,
                    onCreateStory = onCreateStory
                )
            }

            item(key = "guest_stories") {
                VerticalStoriesStrip(
                    currentUserAvatar = "",
                    currentUserName = GUEST_NAME,
                    stories = stories,
                    onCreateStory = onCreateStory,
                    onOpenStory = onOpenStory
                )
            }

            item(key = "guest_filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GUEST_FILTERS) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            if (filteredPosts.isEmpty()) {
                item(key = "guest_welcome") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Chào mừng đến chế độ khách Twin",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Bạn có thể dùng thử đăng bài viết, đính kèm tệp, tạo Tin 24h, học Tiếng Trung · Tiếng Anh và đổi giao diện Sáng/Tối ngay tại đây. Đăng nhập để đồng bộ bảng tin trực tiếp với web Twin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRequestSignIn,
                                modifier = Modifier.testTag("guest_welcome_sign_in")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đăng nhập / Đăng ký ngay")
                            }
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    TwinPostCard(
                        post = post,
                        currentUid = GUEST_UID,
                        onReact = { key -> onReact(post.id, key) },
                        onOpenComments = { onOpenComments(post.id) },
                        onToggleSave = { onToggleSave(post.id) },
                        onShare = { onShare(post) },
                        onDelete = { onDelete(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestSavedContent(
    savedPosts: List<TwinPost>,
    onReact: (String, String) -> Unit,
    onOpenComments: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onShare: (TwinPost) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Bài viết đã lưu (Khách)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (savedPosts.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    FeedStatusCard(
                        title = "Chưa có bài viết đã lưu",
                        subtitle = "Nhấn biểu tượng dấu trang trên bài viết nháp để xem lại tại đây."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedPosts, key = { it.id }) { post ->
                        TwinPostCard(
                            post = post,
                            currentUid = GUEST_UID,
                            onReact = { key -> onReact(post.id, key) },
                            onOpenComments = { onOpenComments(post.id) },
                            onToggleSave = { onToggleSave(post.id) },
                            onShare = { onShare(post) },
                            onDelete = { onDelete(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestProfileContent(
    postCount: Int,
    savedCount: Int,
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onRequestSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                TwinAvatar(avatarUrl = "", name = GUEST_NAME, size = 68.dp, showStoryRing = true)
                                Column {
                                    Text(
                                        text = GUEST_NAME,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Phiên trải nghiệm cục bộ • $postCount bài nháp • $savedCount đã lưu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = onRequestSignIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("guest_profile_sign_in")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đăng nhập để đồng bộ với web Twin")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null)
                            Text(
                                text = stringResource(R.string.profile_dark_mode),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestSignInRequiredScreen(
    title: String,
    subtitle: String,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("guest_gate_sign_in")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng nhập / Đăng ký ngay")
                }
            }
        }
    }
}

@Composable
private fun GuestCreatePostDialog(
    onDismiss: () -> Unit,
    onPublish: (content: String, feeling: String, imageUri: Uri?, fileUri: Uri?, fileName: String) -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri
            fileName = resolveDisplayFileName(context, uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.composer_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(stringResource(R.string.composer_prompt)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GUEST_FEELINGS) { item ->
                        FilterChip(
                            selected = feeling == item,
                            onClick = { feeling = if (feeling == item) "" else item },
                            label = { Text(item) }
                        )
                    }
                }
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Ảnh nháp",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
                if (fileUri != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(fileName.ifBlank { "Tệp đính kèm" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { fileUri = null; fileName = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Bỏ tệp", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ảnh")
                    }
                    OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tệp")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onPublish(content, feeling, imageUri, fileUri, fileName) },
                        enabled = content.isNotBlank() || imageUri != null || fileUri != null
                    ) {
                        Text(stringResource(R.string.composer_publish))
                    }
                }
            }
        }
    }
}
