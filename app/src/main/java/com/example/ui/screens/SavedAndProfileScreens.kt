package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.signOutTwin
import com.example.data.model.TwinUser
import com.example.data.model.UiState
import com.example.ui.TwinMainViewModel
import com.example.ui.components.AdminVerifiedIcon
import com.example.ui.components.CommentsDialog
import com.example.ui.components.TwinAvatar
import com.example.ui.components.TwinPostCard

@Composable
fun SavedPostsScreen(
    viewModel: TwinMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val savedPosts = remember(allPosts, viewModel.currentUserId) {
        allPosts.filter { viewModel.currentUserId in it.savedBy }
    }
    val activeCommentPost = remember(allPosts, activeCommentPostId) {
        allPosts.firstOrNull { it.id == activeCommentPostId }
    }

    Column(
        modifier = modifier
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bài viết đã lưu",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (postsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        FeedStatusCard("Không thể tải bài viết đã lưu", (postsState as UiState.Error).message)
                    }
                }

                is UiState.Success -> {
                    if (savedPosts.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            FeedStatusCard(
                                title = "Chưa có bài viết nào được lưu",
                                subtitle = "Nhấn biểu tượng dấu trang trên bài viết bất kỳ ở Bảng tin để xem lại sau."
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
                                    currentUid = viewModel.currentUserId,
                                    onReact = { key -> viewModel.toggleReaction(post, key) },
                                    onOpenComments = { activeCommentPostId = post.id },
                                    onToggleSave = { viewModel.toggleSavePost(post) },
                                    onShare = {
                                        val shareBody = "${post.authorName} trên Twin:\n${post.content}"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareBody)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Chia sẻ bài viết Twin"))
                                        viewModel.recordShare(post.id)
                                    },
                                    onDelete = if (post.authorUid == viewModel.currentUserId) {
                                        { viewModel.deletePost(post.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeCommentPost != null) {
        CommentsDialog(
            post = activeCommentPost,
            onDismiss = { activeCommentPostId = null },
            onSendComment = { text -> viewModel.addComment(activeCommentPost, text) }
        )
    }
}

@Composable
fun ProfileScreen(
    viewModel: TwinMainViewModel,
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val myPosts = remember(allPosts, viewModel.currentUserId) {
        allPosts.filter { it.authorUid == viewModel.currentUserId }
    }
    val savedCount = remember(allPosts, viewModel.currentUserId) {
        allPosts.count { viewModel.currentUserId in it.savedBy }
    }
    val activeCommentPost = remember(allPosts, activeCommentPostId) {
        allPosts.firstOrNull { it.id == activeCommentPostId }
    }

    Box(
        modifier = modifier
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
            // Profile Hero Card
            item(key = "profile_hero") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Cover photo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                        ) {
                            val coverUrl = currentUser?.cover.orEmpty()
                            if (coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = "Ảnh bìa cá nhân",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                TwinAvatar(
                                    avatarUrl = currentUser?.avatar.orEmpty(),
                                    name = currentUser?.name ?: "Thành viên Twin",
                                    size = 74.dp,
                                    showStoryRing = true,
                                    online = true
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = currentUser?.name ?: "Thành viên Twin",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (currentUser?.role == "admin") AdminVerifiedIcon()
                                    }
                                    Text(
                                        text = currentUser?.bio.orEmpty().ifBlank { "Xin chào, mình đang dùng Twin!" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Optional metadata rows
                            val work = currentUser?.work.orEmpty()
                            val location = currentUser?.location.orEmpty()
                            val website = currentUser?.website.orEmpty()
                            if (work.isNotBlank() || location.isNotBlank() || website.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (work.isNotBlank()) ProfileMetaRow(Icons.Outlined.WorkOutline, work)
                                    if (location.isNotBlank()) ProfileMetaRow(Icons.Outlined.LocationOn, location)
                                    if (website.isNotBlank()) ProfileMetaRow(Icons.Outlined.Language, website)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileStatItem("${currentUser?.friends?.size ?: 0}", "Bạn bè")
                                ProfileStatItem("${myPosts.size}", "Bài viết")
                                ProfileStatItem("$savedCount", "Đã lưu")
                            }

                            Button(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.profile_edit))
                            }
                        }
                    }
                }
            }

            // Preferences & Account Card
            item(key = "profile_settings") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            Switch(
                                checked = darkMode,
                                onCheckedChange = onToggleDarkMode,
                                modifier = Modifier.testTag("dark_mode_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        OutlinedButton(
                            onClick = {
                                signOutTwin(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onBeforeSignOut = { viewModel.markOfflineBeforeSignOut() }
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.profile_sign_out))
                        }
                    }
                }
            }

            // Own Posts Header
            item(key = "my_posts_header") {
                Text(
                    text = "Bài viết của bạn (${myPosts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (myPosts.isEmpty()) {
                item(key = "my_posts_empty") {
                    FeedStatusCard(
                        title = "Bạn chưa đăng bài viết nào",
                        subtitle = "Chuyển sang Bảng tin để chia sẻ khoảnh khắc đầu tiên trên Twin."
                    )
                }
            } else {
                items(myPosts, key = { it.id }) { post ->
                    TwinPostCard(
                        post = post,
                        currentUid = viewModel.currentUserId,
                        onReact = { key -> viewModel.toggleReaction(post, key) },
                        onOpenComments = { activeCommentPostId = post.id },
                        onToggleSave = { viewModel.toggleSavePost(post) },
                        onShare = {
                            val shareBody = "${post.authorName} trên Twin:\n${post.content}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(Intent.createChooser(intent, "Chia sẻ bài viết Twin"))
                            viewModel.recordShare(post.id)
                        },
                        onDelete = { viewModel.deletePost(post.id) }
                    )
                }
            }
        }
    }

    if (showEditDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            isUploading = isUploading,
            onDismiss = { if (!isUploading) showEditDialog = false },
            onSave = { name, bio, location, work, website, avatarUri, coverUri ->
                viewModel.updateProfile(
                    name = name,
                    bio = bio,
                    location = location,
                    work = work,
                    website = website,
                    newAvatarUri = avatarUri,
                    newCoverUri = coverUri
                ) {
                    showEditDialog = false
                }
            }
        )
    }

    if (activeCommentPost != null) {
        CommentsDialog(
            post = activeCommentPost,
            onDismiss = { activeCommentPostId = null },
            onSendComment = { text -> viewModel.addComment(activeCommentPost, text) }
        )
    }
}

@Composable
private fun ProfileMetaRow(icon: ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditProfileDialog(
    user: TwinUser,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        bio: String,
        location: String,
        work: String,
        website: String,
        newAvatarUri: Uri?,
        newCoverUri: Uri?
    ) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var bio by remember { mutableStateOf(user.bio) }
    var location by remember { mutableStateOf(user.location) }
    var work by remember { mutableStateOf(user.work) }
    var website by remember { mutableStateOf(user.website) }
    var newAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var newCoverUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newAvatarUri = uri }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newCoverUri = uri }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.profile_edit),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (newAvatarUri != null) "Đã chọn Avatar" else "Đổi Avatar")
                    }
                    OutlinedButton(
                        onClick = {
                            coverPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (newCoverUri != null) "Đã chọn Ảnh bìa" else "Đổi Ảnh bìa")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hiển thị") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Tiểu sử") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = work,
                    onValueChange = { work = it },
                    label = { Text("Công việc") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Nơi sống") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSave(name, bio, location, work, website, newAvatarUri, newCoverUri)
                    },
                    enabled = !isUploading && name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_profile_button")
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.profile_save_changes))
                }
            }
        }
    }
}
