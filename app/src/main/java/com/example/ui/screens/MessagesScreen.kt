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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.TwinChatMessage
import com.example.data.model.TwinUser
import com.example.data.model.UiState
import com.example.ui.TwinMainViewModel
import com.example.ui.components.TwinAvatar
import com.example.ui.components.formatTimestampVi

@Composable
fun MessagesScreen(
    viewModel: TwinMainViewModel,
    selectedChatUid: String?,
    onSelectChat: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploadingMedia.collectAsStateWithLifecycle()

    val allUsers = (usersState as? UiState.Success)?.data.orEmpty()
    val partner = remember(allUsers, selectedChatUid) {
        allUsers.firstOrNull { it.effectiveUid == selectedChatUid }
    }

    if (selectedChatUid != null) {
        BackHandler(onBack = onClearChat)
        ChatRoomView(
            viewModel = viewModel,
            otherUid = selectedChatUid,
            partner = partner,
            isUploading = isUploading,
            onBack = onClearChat,
            modifier = modifier
        )
    } else {
        ConversationListView(
            currentUid = viewModel.currentUserId,
            friendIds = currentUser?.friends.orEmpty().toSet(),
            usersState = usersState,
            onSelectChat = onSelectChat,
            modifier = modifier
        )
    }
}

@Composable
private fun ConversationListView(
    currentUid: String,
    friendIds: Set<String>,
    usersState: UiState<List<TwinUser>>,
    onSelectChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.chat_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Tìm người để trò chuyện...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_search_input")
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (usersState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        FeedStatusCard("Không thể tải danh bạ Twin Chat", usersState.message)
                    }
                }

                is UiState.Success -> {
                    val q = search.trim().lowercase()
                    val contacts = usersState.data
                        .filter { it.effectiveUid != currentUid }
                        .filter { q.isBlank() || it.name.lowercase().contains(q) }
                        .sortedWith(
                            compareByDescending<TwinUser> { it.effectiveUid in friendIds }
                                .thenByDescending { it.isCurrentlyOnline }
                        )

                    if (contacts.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            FeedStatusCard(
                                title = "Chưa có cuộc trò chuyện nào",
                                subtitle = "Hãy kết nối với thành viên khác để bắt đầu nhắn tin thời gian thực."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 640.dp),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(contacts, key = { it.effectiveUid }) { user ->
                                val isFriend = user.effectiveUid in friendIds
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("chat_contact_${user.effectiveUid}")
                                        .clickable { onSelectChat(user.effectiveUid) },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        TwinAvatar(
                                            avatarUrl = user.avatar,
                                            name = user.name,
                                            size = 48.dp,
                                            online = user.isCurrentlyOnline
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when {
                                                    user.isCurrentlyOnline && isFriend -> "Đang hoạt động • Bạn bè"
                                                    user.isCurrentlyOnline -> "Đang hoạt động • Twin"
                                                    isFriend -> "Bạn bè • Nhấn để nhắn tin"
                                                    else -> "Thành viên Twin"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}

@Composable
private fun ChatRoomView(
    viewModel: TwinMainViewModel,
    otherUid: String,
    partner: TwinUser?,
    isUploading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messagesFlow = remember(otherUid) { viewModel.observeChatWith(otherUid) }
    val messagesState by messagesFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

    var draft by remember { mutableStateOf("") }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingImageUri = uri
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingFileUri = uri
            pendingFileName = resolveDisplayFileName(context, uri)
        }
    }

    val messageList = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Chat Room Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
                TwinAvatar(
                    avatarUrl = partner?.avatar.orEmpty(),
                    name = partner?.name ?: "Twin",
                    size = 38.dp,
                    online = partner?.isCurrentlyOnline == true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partner?.name ?: "Cuộc trò chuyện Twin",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (partner?.isCurrentlyOnline == true) "Đang hoạt động" else "Twin Chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Messages Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        FeedStatusCard("Không thể tải tin nhắn", (messagesState as UiState.Error).message)
                    }
                }

                is UiState.Success -> {
                    if (messageList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hãy gửi lời chào đầu tiên trên Twin Chat!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 640.dp),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messageList, key = { it.id }) { msg ->
                                ChatBubbleItem(
                                    message = msg,
                                    isMine = msg.senderUid == viewModel.currentUserId,
                                    onDelete = { viewModel.deleteChatMessage(otherUid, msg.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending attachment previews (image / file)
        if (pendingImageUri != null || pendingFileUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (pendingImageUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = pendingImageUri,
                                contentDescription = "Ảnh đính kèm",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Đã chọn 1 ảnh",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { pendingImageUri = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Bỏ ảnh", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (pendingFileUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = pendingFileName.ifBlank { "Tệp đính kèm" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    pendingFileUri = null
                                    pendingFileName = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Bỏ tệp", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Message Input Bar (Photo + File + Text + Send)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isUploading,
                    modifier = Modifier.testTag("chat_pick_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Gửi ảnh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    enabled = !isUploading,
                    modifier = Modifier.testTag("chat_pick_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Đính kèm tệp",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                IconButton(
                    onClick = {
                        val textToSend = draft.trim()
                        val img = pendingImageUri
                        val file = pendingFileUri
                        if (textToSend.isNotEmpty() || img != null || file != null) {
                            viewModel.sendChatMessage(
                                otherUid = otherUid,
                                text = textToSend,
                                imageUri = img,
                                fileUri = file,
                                onSent = {
                                    draft = ""
                                    pendingImageUri = null
                                    pendingFileUri = null
                                    pendingFileName = ""
                                }
                            )
                        }
                    },
                    enabled = !isUploading && (draft.isNotBlank() || pendingImageUri != null || pendingFileUri != null),
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: TwinChatMessage,
    isMine: Boolean,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMine) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Thu hồi tin nhắn",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (message.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Ảnh tin nhắn",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                if (message.fileUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMine) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.fileUrl)))
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "Tệp đính kèm",
                                tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = message.fileName.ifBlank { "Tệp đính kèm" },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatTimestampVi(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
