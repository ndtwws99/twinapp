package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TwinGroup
import com.example.data.model.TwinNotification
import com.example.data.model.TwinPage
import com.example.data.model.TwinPost
import com.example.data.model.TwinUser
import com.example.data.model.UiState
import com.example.ui.theme.twinStoryRingBrush

enum class TwinRoute(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Bảng tin", Icons.Filled.Home, Icons.Outlined.Home),
    REELS("reels", "Video", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
    MARKET("market", "Chợ", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    LEARN("learn", "Học tập", Icons.Filled.School, Icons.Outlined.School),
    PROFILE("profile", "Cá nhân", Icons.Filled.Person, Icons.Outlined.PersonOutline),

    // Extended routes accessible via Header & Expandable Right Sidebar
    FRIENDS("friends", "Bạn bè", Icons.Outlined.PeopleOutline, Icons.Outlined.PeopleOutline),
    GROUPS("groups", "Nhóm", Icons.Outlined.Groups, Icons.Outlined.Groups),
    PAGES("pages", "Trang", Icons.Outlined.Flag, Icons.Outlined.Flag),
    MESSAGES("messages", "Twin Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    SAVED("saved", "Đã lưu", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    HELP("help", "Trợ giúp", Icons.AutoMirrored.Outlined.HelpOutline, Icons.AutoMirrored.Outlined.HelpOutline);

    companion object {
        val bottomTabs = listOf(HOME, REELS, MARKET, LEARN, PROFILE)
    }
}

@Composable
fun TwinBrandLogo(size: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(twinStoryRingBrush()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "T",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.56f).sp
        )
    }
}

/**
 * Exact Twin Web Header (`twin-header` in layout.js):
 * Left: Twin Gradient T Logo + Search Pill
 * Right: Twin Chat button (with badge) + Notifications bell (with badge) + Expandable Right Sidebar button (avatar)
 */
@Composable
fun TwinTopHeader(
    currentUser: TwinUser?,
    isGuest: Boolean,
    unreadNotifCount: Int,
    unreadChatCount: Int,
    onLogoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChatClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenRightDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left: Twin Brand Logo + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogoClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("header_twin_logo")
            ) {
                TwinBrandLogo(size = 34.dp)
                Text(
                    text = "Twin",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Center-Left: Search Trigger Pill (like web twin-search-wrap)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onSearchClick)
                    .testTag("header_search_trigger")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Tìm kiếm trên Twin",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tìm trên Twin...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Action Buttons (Chat, Bell, Expandable Right Sidebar Avatar)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier.testTag("header_chat_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadChatCount > 0) {
                                Badge { Text(if (unreadChatCount > 9) "9+" else "$unreadChatCount") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Twin Chat",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isGuest) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("header_notif_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge { Text(if (unreadNotifCount > 9) "9+" else "$unreadNotifCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (unreadNotifCount > 0) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                                contentDescription = "Thông báo",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Right Expandable Menu Trigger (User Avatar + Menu Badge)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenRightDrawer)
                    .testTag("header_right_drawer_button"),
                contentAlignment = Alignment.Center
            ) {
                TwinAvatar(
                    avatarUrl = currentUser?.avatar.orEmpty(),
                    name = if (isGuest) "Khách" else (currentUser?.name ?: "Twin"),
                    size = 38.dp,
                    showStoryRing = !isGuest
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Mở thanh bên phải",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Exact Twin Web Mobile Bottom Bar (`twin-mobile-bar` in layout.js):
 * Bảng tin | Video | Chợ | Học tập | Cá nhân
 */
@Composable
fun TwinBottomBar(
    currentRoute: TwinRoute,
    onSelectRoute: (TwinRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        TwinRoute.bottomTabs.forEach { tab ->
            val selected = currentRoute == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectRoute(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("bottom_tab_${tab.key}")
            )
        }
    }
}

/**
 * Expandable Right Sidebar Drawer ("Thanh bên phải mở rộng") matching Twin web's right sidebar + account dropdown.
 * Slides smoothly from the right edge with Twin Overview stats, iOS-colored menu items, theme toggle, and live Online Contacts.
 */
@Composable
fun TwinRightDrawerOverlay(
    visible: Boolean,
    currentUser: TwinUser?,
    isGuest: Boolean,
    darkMode: Boolean,
    postsCount: Int,
    users: List<TwinUser>,
    groupsCount: Int,
    onDismiss: () -> Unit,
    onNavigate: (TwinRoute) -> Unit,
    onOpenChatWith: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onPasswordReset: () -> Unit,
    onSignOutOrSignIn: () -> Unit
) {
    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TwinRightSidebarContent(
                currentUser = currentUser,
                isGuest = isGuest,
                darkMode = darkMode,
                postsCount = postsCount,
                users = users,
                groupsCount = groupsCount,
                showCloseButton = true,
                onClose = onDismiss,
                onNavigate = { route ->
                    onDismiss()
                    onNavigate(route)
                },
                onOpenChatWith = { uid ->
                    onDismiss()
                    onOpenChatWith(uid)
                },
                onToggleDarkMode = onToggleDarkMode,
                onPasswordReset = {
                    onDismiss()
                    onPasswordReset()
                },
                onSignOutOrSignIn = {
                    onDismiss()
                    onSignOutOrSignIn()
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 290.dp, max = 330.dp)
            )
        }
    }
}

@Composable
fun TwinRightSidebarContent(
    currentUser: TwinUser?,
    isGuest: Boolean,
    darkMode: Boolean,
    postsCount: Int,
    users: List<TwinUser>,
    groupsCount: Int,
    showCloseButton: Boolean,
    onClose: () -> Unit,
    onNavigate: (TwinRoute) -> Unit,
    onOpenChatWith: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onPasswordReset: () -> Unit,
    onSignOutOrSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = if (showCloseButton) 12.dp else 0.dp,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drawer Top Bar & Mini Profile Card
            item(key = "drawer_profile") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigate(TwinRoute.PROFILE) }
                            .padding(4.dp)
                    ) {
                        TwinAvatar(
                            avatarUrl = currentUser?.avatar.orEmpty(),
                            name = if (isGuest) "Khách" else (currentUser?.name ?: "Thành viên Twin"),
                            size = 44.dp,
                            showStoryRing = !isGuest,
                            online = !isGuest
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isGuest) "Chế độ Khách" else (currentUser?.name ?: "Thành viên Twin"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isGuest && currentUser?.role == "admin") AdminVerifiedIcon()
                            }
                            Text(
                                text = if (isGuest) "Chỉ xem · Trải nghiệm cục bộ" else "Xem trang cá nhân",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (showCloseButton) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("right_drawer_close")
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Đóng thanh bên phải")
                        }
                    }
                }
            }

            // TỔNG QUAN TWIN Card (exact web twin-overview-card)
            item(key = "drawer_overview") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TỔNG QUAN TWIN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OverviewStatRow("Bài viết", "$postsCount", MaterialTheme.colorScheme.onSurface)
                        OverviewStatRow("Thành viên", "${users.size}", MaterialTheme.colorScheme.onSurface)
                        OverviewStatRow("Nhóm cộng đồng", "$groupsCount", MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Extended Navigation Links (matching web twin-account-dropdown & left sidebar)
            item(key = "drawer_nav_heading") {
                Text(
                    text = "TIỆN ÍCH & DANH MỤC",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            item(key = "drawer_nav_items") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DrawerMenuItem(
                        icon = Icons.Outlined.PlayCircleOutline,
                        tint = Color(0xFFE11D48),
                        label = "Video & Tin ngắn",
                        onClick = { onNavigate(TwinRoute.REELS) },
                        testTag = "drawer_item_reels"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.Storefront,
                        tint = Color(0xFFD97706),
                        label = "Chợ mua bán Twin",
                        onClick = { onNavigate(TwinRoute.MARKET) },
                        testTag = "drawer_item_market"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.School,
                        tint = Color(0xFF0D9488),
                        label = "Học Trung · Anh",
                        onClick = { onNavigate(TwinRoute.LEARN) },
                        testTag = "drawer_item_learn"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.PeopleOutline,
                        tint = Color(0xFF2563EB),
                        label = "Bạn bè",
                        onClick = { onNavigate(TwinRoute.FRIENDS) },
                        testTag = "drawer_item_friends"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.Groups,
                        tint = Color(0xFF7C3AED),
                        label = "Nhóm",
                        onClick = { onNavigate(TwinRoute.GROUPS) },
                        testTag = "drawer_item_groups"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.Flag,
                        tint = Color(0xFFEA580C),
                        label = "Trang",
                        onClick = { onNavigate(TwinRoute.PAGES) },
                        testTag = "drawer_item_pages"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        tint = Color(0xFF0095F6),
                        label = "Twin Chat",
                        onClick = { onNavigate(TwinRoute.MESSAGES) },
                        testTag = "drawer_item_messages"
                    )
                    DrawerMenuItem(
                        icon = Icons.Outlined.BookmarkBorder,
                        tint = Color(0xFF4F46E5),
                        label = "Bài viết đã lưu",
                        onClick = { onNavigate(TwinRoute.SAVED) },
                        testTag = "drawer_item_saved"
                    )
                    DrawerMenuItem(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        tint = Color(0xFF059669),
                        label = "Trợ giúp & Hướng dẫn",
                        onClick = { onNavigate(TwinRoute.HELP) },
                        testTag = "drawer_item_help"
                    )
                }
            }

            item(key = "drawer_divider_1") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Quick Settings & Account Actions
            item(key = "drawer_settings") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DrawerMenuItem(
                        icon = if (darkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        tint = MaterialTheme.colorScheme.primary,
                        label = if (darkMode) "Chuyển Chế độ Sáng" else "Chuyển Chế độ Tối",
                        onClick = onToggleDarkMode,
                        testTag = "drawer_toggle_theme"
                    )
                    if (!isGuest) {
                        DrawerMenuItem(
                            icon = Icons.Outlined.Key,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Đặt / Đổi mật khẩu",
                            onClick = onPasswordReset,
                            testTag = "drawer_reset_password"
                        )
                    }
                    DrawerMenuItem(
                        icon = if (isGuest) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout,
                        tint = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        label = if (isGuest) "Đăng nhập / Tạo tài khoản" else "Đăng xuất",
                        labelColor = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        onClick = onSignOutOrSignIn,
                        testTag = "drawer_auth_action"
                    )
                }
            }

            // Online Contacts (matching web `Người liên hệ` in right column)
            if (!isGuest && users.isNotEmpty()) {
                item(key = "drawer_contacts_header") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NGƯỜI LIÊN HỆ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { onNavigate(TwinRoute.MESSAGES) }) {
                            Text("Mở Chat", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                val myUid = currentUser?.effectiveUid.orEmpty()
                val contacts = users.filter { it.effectiveUid != myUid }.take(10)
                items(contacts, key = { "contact_${it.effectiveUid}" }) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenChatWith(member.effectiveUid) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TwinAvatar(
                            avatarUrl = member.avatar,
                            name = member.name,
                            size = 34.dp,
                            online = member.isCurrentlyOnline
                        )
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Global Search Overlay matching Twin web's `twin-search-dropdown` searching users, groups, pages, and posts.
 */
@Composable
fun TwinGlobalSearchDialog(
    users: List<TwinUser>,
    posts: List<TwinPost>,
    groups: List<TwinGroup>,
    pages: List<TwinPage>,
    onOpenChatWith: (String) -> Unit,
    onNavigateRoute: (TwinRoute) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    val matchedUsers = remember(users, q) {
        if (q.isBlank()) users.take(6) else users.filter { it.name.lowercase().contains(q) || it.bio.lowercase().contains(q) }.take(8)
    }
    val matchedGroups = remember(groups, q) {
        if (q.isBlank()) emptyList() else groups.filter { it.name.lowercase().contains(q) }.take(5)
    }
    val matchedPages = remember(pages, q) {
        if (q.isBlank()) emptyList() else pages.filter { it.name.lowercase().contains(q) }.take(5)
    }
    val matchedPosts = remember(posts, q) {
        if (q.isBlank()) emptyList() else posts.filter {
            it.content.lowercase().contains(q) ||
                it.marketTitle.lowercase().contains(q) ||
                it.authorName.lowercase().contains(q)
        }.take(6)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f)
                .widthIn(max = 540.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Tìm thành viên, bài viết, nhóm, chợ...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("global_search_input")
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Đóng tìm kiếm")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (matchedUsers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Thành viên Twin",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchedUsers, key = { "su_${it.effectiveUid}" }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onDismiss()
                                        onOpenChatWith(user.effectiveUid)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TwinAvatar(user.avatar, user.name, size = 38.dp, online = user.isCurrentlyOnline)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        text = user.bio.ifBlank { "Nhấn để nhắn tin" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (matchedGroups.isNotEmpty()) {
                        item {
                            Text(
                                text = "Nhóm",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchedGroups, key = { "sg_${it.id}" }) { group ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onNavigateRoute(TwinRoute.GROUPS)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(group.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${group.members.size} thành viên", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (matchedPages.isNotEmpty()) {
                        item {
                            Text(
                                text = "Trang",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchedPages, key = { "sp_${it.id}" }) { page ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onNavigateRoute(TwinRoute.PAGES)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(page.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${page.category} • ${page.followers.size} theo dõi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (matchedPosts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Bài viết & Sản phẩm",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchedPosts, key = { "spost_${it.id}" }) { post ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onNavigateRoute(if (post.isMarketPost) TwinRoute.MARKET else TwinRoute.HOME)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (post.isMarketPost) post.marketTitle else post.authorName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = post.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (q.isNotBlank() && matchedUsers.isEmpty() && matchedGroups.isEmpty() && matchedPages.isEmpty() && matchedPosts.isEmpty()) {
                        item {
                            Text(
                                text = "Không tìm thấy kết quả nào cho \"$query\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TwinNotificationsDialog(
    notificationsState: UiState<List<TwinNotification>>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenNotification: (TwinNotification) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.76f)
                .widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Thông báo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onMarkAllRead) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đọc tất cả", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Đóng")
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                when (notificationsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(notificationsState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is UiState.Success -> {
                        if (notificationsState.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Chưa có thông báo mới nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(notificationsState.data, key = { it.id }) { notif ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (notif.read) {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!notif.read) onMarkRead(notif.id)
                                                onOpenNotification(notif)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TwinAvatar(
                                                avatarUrl = notif.fromAvatar,
                                                name = notif.fromName,
                                                size = 40.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${notif.fromName} ${notif.displayBody}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (notif.createdAt != null) {
                                                        formatTimestampVi(notif.createdAt)
                                                    } else {
                                                        formatMillisVi(notif.createdAtMs)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!notif.read) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
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
}
