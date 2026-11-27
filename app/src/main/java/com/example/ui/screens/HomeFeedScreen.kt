package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.TwinStory
import com.example.data.model.UiState
import com.example.ui.TwinMainViewModel
import com.example.ui.components.CommentsDialog
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.TwinAvatar
import com.example.ui.components.TwinPostCard
import com.example.ui.theme.storyBackgroundBrush

private val FEELING_OPTIONS = listOf("Vui vẻ", "Hào hứng", "Đang code", "Hạnh phúc", "Thư giãn")
private val STORY_BG_PRESETS = listOf("ig", "ocean", "emerald", "sunset", "midnight")
private val FEED_FILTERS = listOf("Tất cả", "Ảnh", "Chợ Twin", "Tệp đính kèm")

@Composable
fun HomeFeedScreen(
    viewModel: TwinMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val isUploadingMedia by viewModel.isUploadingMedia.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var showCreatePost by remember { mutableStateOf(false) }
    var showCreateStory by remember { mutableStateOf(false) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var activeStoryId by remember { mutableStateOf<String?>(null) }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val allStories = (storiesState as? UiState.Success)?.data.orEmpty()
    val filteredPosts = remember(allPosts, selectedFilter) {
        when (selectedFilter) {
            "Ảnh" -> allPosts.filter { it.primaryImageUrl.isNotBlank() }
            "Chợ Twin" -> allPosts.filter { it.isMarketPost }
            "Tệp đính kèm" -> allPosts.filter { it.fileUrl.isNotBlank() }
            else -> allPosts
        }
    }
    val activeCommentPost = remember(allPosts, activeCommentPostId) {
        allPosts.firstOrNull { it.id == activeCommentPostId }
    }
    val activeStory = remember(allStories, activeStoryId) {
        allStories.firstOrNull { it.id == activeStoryId }
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
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Composer Card FIRST (matching web index.html)
            item(key = "composer_card") {
                WebStyleComposerCard(
                    avatarUrl = currentUser?.avatar.orEmpty(),
                    userName = currentUser?.name ?: "Twin",
                    onOpenComposer = { showCreatePost = true },
                    onCreateStory = { showCreateStory = true }
                )
            }

            // 2. Vertical Stories 24h Strip SECOND (matching web twin-stories-strip)
            item(key = "stories_strip") {
                VerticalStoriesStrip(
                    currentUserAvatar = currentUser?.avatar.orEmpty(),
                    currentUserName = currentUser?.name ?: "Bạn",
                    stories = allStories,
                    onCreateStory = { showCreateStory = true },
                    onOpenStory = { activeStoryId = it.id }
                )
            }

            // 3. Feed Filter Bar THIRD (matching web feed filter bar)
            item(key = "feed_filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FEED_FILTERS) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            modifier = Modifier.testTag("feed_filter_$filter")
                        )
                    }
                }
            }

            // 4. Live Feed Posts
            when (val state = postsState) {
                is UiState.Loading -> {
                    item(key = "posts_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "posts_error") {
                        FeedStatusCard(
                            title = "Không thể tải bảng tin Twin",
                            subtitle = state.message
                        )
                    }
                }

                is UiState.Success -> {
                    if (filteredPosts.isEmpty()) {
                        item(key = "posts_empty") {
                            FeedStatusCard(
                                title = if (selectedFilter == "Tất cả") {
                                    "Bảng tin Twin chưa có bài viết nào"
                                } else {
                                    "Chưa có bài viết thuộc mục \"$selectedFilter\""
                                },
                                subtitle = "Hãy tạo bài viết, đính kèm ảnh hoặc tệp đầu tiên để đồng bộ trực tiếp với web Twin!"
                            )
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            TwinPostCard(
                                post = post,
                                currentUid = viewModel.currentUserId,
                                onReact = { key -> viewModel.toggleReaction(post, key) },
                                onOpenComments = { activeCommentPostId = post.id },
                                onToggleSave = { viewModel.toggleSavePost(post) },
                                onShare = {
                                    val shareBody = buildString {
                                        append("${post.authorName} trên Twin:\n")
                                        if (post.content.isNotBlank()) append(post.content)
                                        if (post.primaryImageUrl.isNotBlank()) append("\n${post.primaryImageUrl}")
                                        if (post.fileUrl.isNotBlank()) append("\n${post.fileUrl}")
                                    }
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

    if (showCreatePost) {
        CreatePostDialog(
            isUploading = isUploadingMedia,
            onDismiss = { if (!isUploadingMedia) showCreatePost = false },
            onPublish = { content, feeling, privacy, imageUri, fileUri ->
                viewModel.createPost(content, feeling, privacy, imageUri, fileUri) {
                    showCreatePost = false
                }
            }
        )
    }

    if (showCreateStory) {
        CreateStoryDialog(
            isUploading = isUploadingMedia,
            onDismiss = { if (!isUploadingMedia) showCreateStory = false },
            onPublish = { text, bgId, imageUri, caption ->
                viewModel.createStory(text, bgId, imageUri, caption) {
                    showCreateStory = false
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

    if (activeStory != null) {
        StoryViewerDialog(
            story = activeStory,
            currentUid = viewModel.currentUserId,
            onToggleLike = { viewModel.toggleStoryLike(activeStory) },
            onDismiss = { activeStoryId = null }
        )
    }
}

@Composable
fun WebStyleComposerCard(
    avatarUrl: String,
    userName: String,
    onOpenComposer: () -> Unit,
    onCreateStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_composer_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenComposer)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TwinAvatar(
                    avatarUrl = avatarUrl,
                    name = userName,
                    size = 40.dp
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.composer_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComposerShortcutAction(
                    icon = Icons.Outlined.AddPhotoAlternate,
                    label = "Ảnh",
                    tint = Color(0xFF16A34A),
                    onClick = onOpenComposer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("composer_photo_shortcut")
                )
                ComposerShortcutAction(
                    icon = Icons.Outlined.AttachFile,
                    label = "Tệp",
                    tint = Color(0xFF2563EB),
                    onClick = onOpenComposer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("composer_file_shortcut")
                )
                ComposerShortcutAction(
                    icon = Icons.Outlined.AutoStories,
                    label = "Tin 24h",
                    tint = Color(0xFFE11D48),
                    onClick = onCreateStory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("composer_story_shortcut")
                )
            }
        }
    }
}

@Composable
private fun ComposerShortcutAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Vertical 24h Story Cards Strip matching web `twin-stories-strip` (96dp x 148dp rounded cards).
 */
@Composable
fun VerticalStoriesStrip(
    currentUserAvatar: String,
    currentUserName: String,
    stories: List<TwinStory>,
    onCreateStory: () -> Unit,
    onOpenStory: (TwinStory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Create Story Vertical Card
        item(key = "create_story_card") {
            Card(
                modifier = Modifier
                    .width(98.dp)
                    .height(152.dp)
                    .testTag("create_story_button")
                    .clickable(onClick = onCreateStory),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUserAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = currentUserAvatar,
                                    contentDescription = currentUserName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                TwinAvatar(avatarUrl = "", name = currentUserName, size = 46.dp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(top = 14.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = stringResource(R.string.stories_create),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Overlapping + Circle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.stories_create),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Story Preview Cards
        items(stories, key = { it.id }) { story ->
            Card(
                modifier = Modifier
                    .width(98.dp)
                    .height(152.dp)
                    .testTag("story_item_${story.id}")
                    .clickable { onOpenStory(story) },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(storyBackgroundBrush(story.bgId))
                ) {
                    if (story.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = story.imageUrl,
                            contentDescription = "Tin của ${story.authorName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (story.text.isNotBlank()) {
                        Text(
                            text = story.text,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 8.dp)
                        )
                    }

                    // Bottom readability scrim
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                )
                            )
                    )

                    // Top-left author avatar with story ring
                    Box(modifier = Modifier.padding(8.dp)) {
                        TwinAvatar(
                            avatarUrl = story.authorAvatar,
                            name = story.authorName,
                            size = 32.dp,
                            showStoryRing = true
                        )
                    }

                    // Bottom author name
                    Text(
                        text = story.authorName.ifBlank { "Twin" },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onPublish: (content: String, feeling: String, privacy: String, imageUri: Uri?, fileUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var selectedFeeling by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("public") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = resolveDisplayFileName(context, uri)
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
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                // Privacy Selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = privacy == "public",
                        onClick = { privacy = "public" },
                        label = { Text("Công khai") },
                        leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = privacy == "friends",
                        onClick = { privacy = "friends" },
                        label = { Text("Bạn bè") },
                        leadingIcon = { Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(stringResource(R.string.composer_prompt)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_content_input")
                )

                // Feeling chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FEELING_OPTIONS) { feeling ->
                        FilterChip(
                            selected = selectedFeeling == feeling,
                            onClick = {
                                selectedFeeling = if (selectedFeeling == feeling) "" else feeling
                            },
                            label = { Text(feeling) }
                        )
                    }
                }

                // Selected Image Preview
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Ảnh đã chọn",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Bỏ ảnh", tint = Color.White)
                        }
                    }
                }

                // Selected File Preview Chip
                if (selectedFileUri != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = selectedFileName.ifBlank { "Tệp đính kèm" },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    selectedFileUri = null
                                    selectedFileName = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Bỏ tệp", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading,
                        modifier = Modifier.testTag("create_post_pick_photo_button")
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ảnh")
                    }

                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !isUploading,
                        modifier = Modifier.testTag("create_post_pick_file_button")
                    ) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tệp")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            onPublish(content, selectedFeeling, privacy, selectedImageUri, selectedFileUri)
                        },
                        enabled = !isUploading && (content.isNotBlank() || selectedImageUri != null || selectedFileUri != null),
                        modifier = Modifier.testTag("create_post_publish_button")
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.composer_publish))
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStoryDialog(
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onPublish: (text: String, bgId: String, imageUri: Uri?, caption: String) -> Unit
) {
    var modeImage by remember { mutableStateOf(false) }
    var storyText by remember { mutableStateOf("") }
    var bgId by remember { mutableStateOf("ig") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            modeImage = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp),
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
                        text = stringResource(R.string.story_create_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !modeImage,
                        onClick = { modeImage = false; imageUri = null },
                        label = { Text("Tin chữ") }
                    )
                    FilterChip(
                        selected = modeImage,
                        onClick = {
                            modeImage = true
                            if (imageUri == null) {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        label = { Text("Tin ảnh") }
                    )
                }

                if (!modeImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(storyBackgroundBrush(bgId))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = storyText.ifBlank { "Chia sẻ khoảnh khắc 24h..." },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        STORY_BG_PRESETS.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(storyBackgroundBrush(preset))
                                    .border(
                                        width = if (bgId == preset) 3.dp else 1.dp,
                                        color = if (bgId == preset) MaterialTheme.colorScheme.primary else Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { bgId = preset }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        placeholder = { Text("Nhập nội dung tin...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("story_text_input")
                    )
                } else {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Ảnh tin 24h",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (imageUri == null) "Chọn ảnh cho Tin 24h" else "Đổi ảnh khác")
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Chú thích ngắn (không bắt buộc)...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        onPublish(
                            if (modeImage) "" else storyText,
                            bgId,
                            if (modeImage) imageUri else null,
                            caption
                        )
                    },
                    enabled = !isUploading && (if (modeImage) imageUri != null else storyText.isNotBlank()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("story_publish_button")
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.story_publish))
                }
            }
        }
    }
}

@Composable
fun FeedStatusCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

internal fun resolveDisplayFileName(context: Context, uri: Uri): String {
    var name = ""
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx).orEmpty()
            }
        }
    }
    return name.ifBlank { uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "tep_dinh_kem" } }
}
