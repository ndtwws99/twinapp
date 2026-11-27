package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.TwinGroup
import com.example.data.model.TwinPage
import com.example.data.model.TwinUser
import com.example.data.model.UiState
import com.example.ui.TwinMainViewModel
import com.example.ui.components.AdminVerifiedIcon
import com.example.ui.components.TwinAvatar

@Composable
fun CommunityScreen(
    viewModel: TwinMainViewModel,
    initialTab: Int = 0,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
    val pagesState by viewModel.pagesState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showCreatePage by remember { mutableStateOf(false) }

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
                    text = stringResource(R.string.nav_community),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text(stringResource(R.string.community_friends)) },
                        modifier = Modifier.testTag("community_tab_friends")
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text(stringResource(R.string.community_groups)) },
                        modifier = Modifier.testTag("community_tab_groups")
                    )
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text(stringResource(R.string.community_pages)) },
                        modifier = Modifier.testTag("community_tab_pages")
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> FriendsSection(
                    currentUser = currentUser,
                    currentUid = viewModel.currentUserId,
                    usersState = usersState,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onAccept = { viewModel.acceptFriendRequest(it) },
                    onDecline = { viewModel.declineFriendRequest(it) },
                    onSendRequest = { viewModel.sendFriendRequest(it) },
                    onCancelRequest = { viewModel.cancelFriendRequest(it) },
                    onUnfriend = { viewModel.unfriend(it) },
                    onOpenChat = onOpenChat
                )

                1 -> GroupsSection(
                    currentUid = viewModel.currentUserId,
                    groupsState = groupsState,
                    onCreateGroup = { showCreateGroup = true },
                    onToggleJoin = { viewModel.toggleJoinGroup(it) }
                )

                else -> PagesSection(
                    currentUid = viewModel.currentUserId,
                    pagesState = pagesState,
                    onCreatePage = { showCreatePage = true },
                    onToggleFollow = { viewModel.toggleFollowPage(it) }
                )
            }
        }
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onCreate = { name, desc ->
                viewModel.createGroup(name, desc) { showCreateGroup = false }
            }
        )
    }

    if (showCreatePage) {
        CreatePageDialog(
            onDismiss = { showCreatePage = false },
            onCreate = { name, category, desc ->
                viewModel.createPage(name, category, desc) { showCreatePage = false }
            }
        )
    }
}

@Composable
private fun FriendsSection(
    currentUser: TwinUser?,
    currentUid: String,
    usersState: UiState<List<TwinUser>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
    onUnfriend: (String) -> Unit,
    onOpenChat: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "friends_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Tìm kiếm thành viên Twin...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friends_search_input")
            )
        }

        when (usersState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item { FeedStatusCard("Không thể tải thành viên", usersState.message) }
            }

            is UiState.Success -> {
                val query = searchQuery.trim().lowercase()
                val others = usersState.data.filter { user ->
                    user.effectiveUid != currentUid &&
                        (query.isBlank() || user.name.lowercase().contains(query) || user.bio.lowercase().contains(query))
                }
                val incomingIds = currentUser?.friendRequests.orEmpty().toSet()
                val friendIds = currentUser?.friends.orEmpty().toSet()

                val incomingUsers = others.filter { it.effectiveUid in incomingIds }
                val friendsList = others.filter { it.effectiveUid in friendIds }
                val suggestions = others.filter { it.effectiveUid !in incomingIds && it.effectiveUid !in friendIds }

                if (incomingUsers.isNotEmpty()) {
                    item(key = "header_requests") {
                        Text(
                            text = "Lời mời kết bạn (${incomingUsers.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(incomingUsers, key = { "req_${it.effectiveUid}" }) { user ->
                        MemberCard(
                            user = user,
                            actions = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onAccept(user.effectiveUid) },
                                        modifier = Modifier.testTag("accept_friend_${user.effectiveUid}")
                                    ) {
                                        Text(stringResource(R.string.friend_accept))
                                    }
                                    OutlinedButton(onClick = { onDecline(user.effectiveUid) }) {
                                        Text(stringResource(R.string.friend_decline))
                                    }
                                }
                            }
                        )
                    }
                }

                if (friendsList.isNotEmpty()) {
                    item(key = "header_friends") {
                        Text(
                            text = "Bạn bè của bạn (${friendsList.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(friendsList, key = { "friend_${it.effectiveUid}" }) { user ->
                        MemberCard(
                            user = user,
                            actions = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onOpenChat(user.effectiveUid) },
                                        modifier = Modifier.testTag("chat_friend_${user.effectiveUid}")
                                    ) {
                                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nhắn tin")
                                    }
                                    TextButton(onClick = { onUnfriend(user.effectiveUid) }) {
                                        Text(stringResource(R.string.friend_unfriend))
                                    }
                                }
                            }
                        )
                    }
                }

                item(key = "header_discover") {
                    Text(
                        text = "Khám phá thành viên Twin (${suggestions.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (suggestions.isEmpty()) {
                    item(key = "empty_discover") {
                        FeedStatusCard(
                            title = "Chưa có thêm thành viên gợi ý",
                            subtitle = "Khi có thành viên mới đăng nhập vào Twin, họ sẽ xuất hiện tại đây."
                        )
                    }
                } else {
                    items(suggestions, key = { "sug_${it.effectiveUid}" }) { user ->
                        val alreadySent = currentUid in user.friendRequests
                        MemberCard(
                            user = user,
                            actions = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (alreadySent) {
                                        OutlinedButton(onClick = { onCancelRequest(user.effectiveUid) }) {
                                            Text(stringResource(R.string.friend_sent))
                                        }
                                    } else {
                                        Button(
                                            onClick = { onSendRequest(user.effectiveUid) },
                                            modifier = Modifier.testTag("add_friend_${user.effectiveUid}")
                                        ) {
                                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.friend_add))
                                        }
                                    }
                                    OutlinedButton(onClick = { onOpenChat(user.effectiveUid) }) {
                                        Text("Nhắn tin")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    user: TwinUser,
    actions: @Composable () -> Unit
) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.role == "admin") AdminVerifiedIcon()
                    }
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            actions()
        }
    }
}

@Composable
private fun GroupsSection(
    currentUid: String,
    groupsState: UiState<List<TwinGroup>>,
    onCreateGroup: () -> Unit,
    onToggleJoin: (TwinGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "create_group_btn") {
            Button(
                onClick = onCreateGroup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_group_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.group_create))
            }
        }

        when (groupsState) {
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> item { FeedStatusCard("Không thể tải nhóm", groupsState.message) }
            is UiState.Success -> {
                if (groupsState.data.isEmpty()) {
                    item {
                        FeedStatusCard(
                            title = "Chưa có nhóm cộng đồng nào",
                            subtitle = "Tạo nhóm đầu tiên theo sở thích của bạn trên Twin."
                        )
                    }
                } else {
                    items(groupsState.data, key = { it.id }) { group ->
                        val joined = currentUid in group.members
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (group.description.isNotBlank()) {
                                        Text(
                                            text = group.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${group.members.size} thành viên",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (joined) {
                                    OutlinedButton(onClick = { onToggleJoin(group) }) {
                                        Text(stringResource(R.string.group_leave))
                                    }
                                } else {
                                    Button(onClick = { onToggleJoin(group) }) {
                                        Text(stringResource(R.string.group_join))
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
private fun PagesSection(
    currentUid: String,
    pagesState: UiState<List<TwinPage>>,
    onCreatePage: () -> Unit,
    onToggleFollow: (TwinPage) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "create_page_btn") {
            Button(
                onClick = onCreatePage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_page_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.page_create))
            }
        }

        when (pagesState) {
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> item { FeedStatusCard("Không thể tải danh sách Trang", pagesState.message) }
            is UiState.Success -> {
                if (pagesState.data.isEmpty()) {
                    item {
                        FeedStatusCard(
                            title = "Chưa có Trang nào",
                            subtitle = "Tạo Trang thương hiệu hoặc blog của riêng bạn trên Twin."
                        )
                    }
                } else {
                    items(pagesState.data, key = { it.id }) { page ->
                        val following = currentUid in page.followers
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Storefront,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = page.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${page.category} • ${page.followers.size} người theo dõi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (page.description.isNotBlank()) {
                                        Text(
                                            text = page.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (following) {
                                    OutlinedButton(onClick = { onToggleFollow(page) }) {
                                        Text(stringResource(R.string.page_following))
                                    }
                                } else {
                                    Button(onClick = { onToggleFollow(page) }) {
                                        Text(stringResource(R.string.page_follow))
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
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.group_create),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên nhóm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onCreate(name, description) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("group_submit_button")
                ) {
                    Text(stringResource(R.string.group_create))
                }
            }
        }
    }
}

@Composable
private fun CreatePageDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Công nghệ") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.page_create),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Trang") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("page_name_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Danh mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Giới thiệu Trang") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onCreate(name, category, description) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("page_submit_button")
                ) {
                    Text(stringResource(R.string.page_create))
                }
            }
        }
    }
}
