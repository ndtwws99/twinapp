package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.TwinPost
import com.example.data.model.TwinStory
import com.example.data.model.UiState
import com.example.data.model.formatVndPrice
import com.example.ui.components.TwinAvatar
import com.example.ui.components.formatTimestampVi
import com.example.ui.theme.TwinHeartRed
import com.example.ui.theme.storyBackgroundBrush
import kotlinx.coroutines.delay
import java.util.Locale

private val MARKET_CATEGORIES = listOf("Tất cả", "Điện tử", "Thời trang", "Gia dụng", "Sách & Học tập", "Xe cộ", "Khác")

data class FlashcardItem(
    val id: String,
    val lang: String, // "zh" or "en"
    val front: String,
    val phonetic: String,
    val meaning: String,
    val example: String
)

private val DEFAULT_FLASHCARDS = listOf(
    FlashcardItem("zh1", "zh", "你好", "nǐ hǎo", "Xin chào", "你好，很高兴认识你！"),
    FlashcardItem("zh2", "zh", "谢谢", "xiè xie", "Cảm ơn", "非常谢谢你的帮助。"),
    FlashcardItem("zh3", "zh", "学习", "xué xí", "Học tập", "我每天都学习中文。"),
    FlashcardItem("zh4", "zh", "朋友", "péng you", "Bạn bè", "他是我的好朋友。"),
    FlashcardItem("zh5", "zh", "咖啡", "kā fēi", "Cà phê", "我想喝一杯冰咖啡。"),
    FlashcardItem("zh6", "zh", "加油", "jiā yóu", "Cố lên!", "明天考试，加油！"),
    FlashcardItem("en1", "en", "Resilient", "/rɪˈzɪl.jənt/", "Kiên cường, bền bỉ", "She is resilient in the face of challenges."),
    FlashcardItem("en2", "en", "Collaborate", "/kəˈlæb.ə.reɪt/", "Hợp tác cùng nhau", "Teams collaborate better with clear goals."),
    FlashcardItem("en3", "en", "Momentum", "/məˈmen.təm/", "Đà phát triển", "Keep the momentum going every day."),
    FlashcardItem("en4", "en", "Insightful", "/ˈɪn.saɪt.fəl/", "Sâu sắc, thấu đáo", "Thank you for your insightful feedback."),
    FlashcardItem("en5", "en", "Authentic", "/ɔːˈθen.tɪk/", "Chân thật, nguyên bản", "Build an authentic community on Twin."),
    FlashcardItem("en6", "en", "Breakthrough", "/ˈbreɪk.θruː/", "Bước đột phá", "Daily practice leads to a breakthrough.")
)

/**
 * Video & Tin ngắn (Reels) Screen matching web reels.html
 */
@Composable
fun ReelsVideoScreen(
    storiesState: UiState<List<TwinStory>>,
    postsState: UiState<List<TwinPost>>,
    currentUid: String,
    onToggleStoryLike: (TwinStory) -> Unit,
    onCreateStory: () -> Unit,
    onOpenVideoUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stories = (storiesState as? UiState.Success)?.data.orEmpty()
    val videoPosts = remember(postsState) {
        (postsState as? UiState.Success)?.data.orEmpty()
            .filter { it.videoUrl.isNotBlank() || it.primaryImageUrl.isNotBlank() }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "reels_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Video & Tin ngắn Twin",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Khoảnh khắc 24h và bài đăng nổi bật",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onCreateStory,
                        modifier = Modifier.testTag("reels_create_story_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tạo tin")
                    }
                }
            }

            if (storiesState is UiState.Loading && postsState is UiState.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (stories.isEmpty() && videoPosts.isEmpty()) {
                item {
                    FeedStatusCard(
                        title = "Chưa có Video hoặc Tin ngắn nào",
                        subtitle = "Nhấn \"Tạo tin\" để chia sẻ khoảnh khắc đầu tiên lên Twin!"
                    )
                }
            } else {
                items(stories, key = { "reel_story_${it.id}" }) { story ->
                    val liked = currentUid in story.likes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .background(storyBackgroundBrush(story.bgId))
                        ) {
                            if (story.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = story.imageUrl,
                                    contentDescription = "Tin của ${story.authorName}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = story.text.ifBlank { story.caption },
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            if (story.videoUrl.isNotBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(64.dp)
                                        .clickable { onOpenVideoUrl(story.videoUrl) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.PlayArrow,
                                            contentDescription = "Phát video",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // Top overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.38f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TwinAvatar(story.authorAvatar, story.authorName, size = 36.dp, showStoryRing = true)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(story.authorName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text(formatTimestampVi(story.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            // Bottom overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = story.caption.ifBlank { "${story.likes.size} lượt yêu thích" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onToggleStoryLike(story) }) {
                                    Icon(
                                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Thả tim",
                                        tint = if (liked) TwinHeartRed else Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Tin trên Twin của ${story.authorName}: ${story.text.ifBlank { story.caption }}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ tin Twin"))
                                }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Chia sẻ", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                items(videoPosts.filter { it.videoUrl.isNotBlank() }, key = { "reel_vid_${it.id}" }) { post ->
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
                                    .height(220.dp)
                                    .background(Color(0xFF0F172A))
                                    .clickable { onOpenVideoUrl(post.videoUrl) },
                                contentAlignment = Alignment.Center
                            ) {
                                val poster = post.videoPoster.ifBlank { post.primaryImageUrl }
                                if (poster.isNotBlank()) {
                                    AsyncImage(
                                        model = poster,
                                        contentDescription = "Video của ${post.authorName}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(60.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.PlayArrow, contentDescription = "Xem video", tint = Color.White, modifier = Modifier.size(34.dp))
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(post.authorName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                if (post.content.isNotBlank()) {
                                    Text(post.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chợ Twin (Marketplace) Screen matching web market.html 1:1 (reads/writes `posts` with `postType == "market"`).
 */
@Composable
fun MarketScreen(
    postsState: UiState<List<TwinPost>>,
    currentUid: String,
    isGuest: Boolean,
    isUploading: Boolean,
    onCreateProduct: (
        title: String,
        price: Long,
        category: String,
        condition: String,
        location: String,
        description: String,
        imageUri: Uri?,
        onDone: () -> Unit
    ) -> Unit,
    onMessageSeller: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onRequireSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var search by remember { mutableStateOf("") }
    var showSellDialog by remember { mutableStateOf(false) }
    var showCartDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<TwinPost?>(null) }
    var cartItems by remember { mutableStateOf<Map<String, Pair<TwinPost, Int>>>(emptyMap()) }

    val marketProducts = remember(postsState, selectedCategory, search) {
        val all = (postsState as? UiState.Success)?.data.orEmpty().filter { it.isMarketPost }
        val q = search.trim().lowercase()
        all.filter { item ->
            val catOk = selectedCategory == "Tất cả" || item.marketCategory.equals(selectedCategory, ignoreCase = true)
            val qOk = q.isBlank() ||
                item.marketTitle.lowercase().contains(q) ||
                item.content.lowercase().contains(q) ||
                item.marketLocation.lowercase().contains(q)
            catOk && qOk
        }
    }
    val cartCount = remember(cartItems) { cartItems.values.sumOf { it.second } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "market_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Chợ Twin",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Mua bán & trao đổi đồng bộ trực tiếp với web",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showCartDialog = true },
                            modifier = Modifier.testTag("market_cart_button")
                        ) {
                            BadgedBox(badge = { if (cartCount > 0) Badge { Text("$cartCount") } }) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = "Giỏ hàng")
                            }
                        }
                        Button(
                            onClick = {
                                if (isGuest) onRequireSignIn() else showSellDialog = true
                            },
                            modifier = Modifier.testTag("market_sell_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đăng bán")
                        }
                    }
                }
            }

            item(key = "market_search") {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Tìm sản phẩm trên Chợ Twin...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "market_cats") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MARKET_CATEGORIES) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            when (postsState) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> item {
                    FeedStatusCard("Không thể tải Chợ Twin", postsState.message)
                }

                is UiState.Success -> {
                    if (marketProducts.isEmpty()) {
                        item {
                            FeedStatusCard(
                                title = "Chưa có sản phẩm nào trong mục này",
                                subtitle = "Nhấn \"Đăng bán\" để rao bán món đồ đầu tiên lên Chợ Twin!"
                            )
                        }
                    } else {
                        val rows = marketProducts.chunked(2)
                        items(rows, key = { row -> row.joinToString("_") { it.id } }) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { product ->
                                    MarketProductCard(
                                        product = product,
                                        onClick = { selectedProduct = product },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedProduct != null) {
        val item = selectedProduct!!
        Dialog(
            onDismissRequest = { selectedProduct = null },
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
                        Text("Chi tiết sản phẩm", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(onClick = { selectedProduct = null }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Đóng")
                        }
                    }
                    if (item.primaryImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.primaryImageUrl,
                            contentDescription = item.marketTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.3f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    Text(
                        text = item.marketTitle.ifBlank { "Sản phẩm Twin" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formatVndPrice(item.marketPrice),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.marketCategory.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(item.marketCategory, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        if (item.marketCondition.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Text(item.marketCondition, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                    if (item.marketLocation.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.marketLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (item.content.isNotBlank()) {
                        Text(item.content, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TwinAvatar(item.authorAvatar, item.authorName, size = 38.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.authorName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Người bán trên Twin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.authorUid == currentUid) {
                            IconButton(onClick = {
                                onDeleteProduct(item.id)
                                selectedProduct = null
                            }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa tin bán", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val existing = cartItems[item.id]
                                val nextQty = (existing?.second ?: 0) + 1
                                cartItems = cartItems + (item.id to (item to nextQty))
                                selectedProduct = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Thêm vào giỏ")
                        }
                        if (item.authorUid.isNotBlank() && item.authorUid != currentUid) {
                            Button(
                                onClick = {
                                    selectedProduct = null
                                    if (isGuest) onRequireSignIn() else onMessageSeller(item.authorUid)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nhắn người bán")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSellDialog) {
        SellProductDialog(
            isUploading = isUploading,
            onDismiss = { if (!isUploading) showSellDialog = false },
            onPublish = { title, price, cat, cond, loc, desc, uri ->
                onCreateProduct(title, price, cat, cond, loc, desc, uri) {
                    showSellDialog = false
                }
            }
        )
    }

    if (showCartDialog) {
        Dialog(onDismissRequest = { showCartDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Giỏ hàng của bạn ($cartCount)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(onClick = { showCartDialog = false }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Đóng")
                        }
                    }
                    if (cartItems.isEmpty()) {
                        Text("Giỏ hàng đang trống.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val total = cartItems.values.sumOf { it.first.marketPrice * it.second }
                        cartItems.values.forEach { (prod, qty) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.marketTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatVndPrice(prod.marketPrice)} × $qty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { cartItems = cartItems - prod.id }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Bỏ khỏi giỏ")
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tổng tạm tính:", style = MaterialTheme.typography.titleSmall)
                            Text(formatVndPrice(total), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = {
                                val firstSeller = cartItems.values.firstOrNull()?.first?.authorUid.orEmpty()
                                cartItems = emptyMap()
                                showCartDialog = false
                                if (firstSeller.isNotBlank() && firstSeller != currentUid) {
                                    if (isGuest) onRequireSignIn() else onMessageSeller(firstSeller)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Liên hệ đặt mua qua Twin Chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketProductCard(
    product: TwinPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (product.primaryImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.primaryImageUrl,
                        contentDescription = product.marketTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatVndPrice(product.marketPrice),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = product.marketTitle.ifBlank { product.content },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.marketLocation.ifBlank { product.marketCategory.ifBlank { "Chợ Twin" } },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SellProductDialog(
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onPublish: (String, Long, String, String, String, String, Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Điện tử") }
    var condition by remember { mutableStateOf("Mới 99%") }
    var location by remember { mutableStateOf("Hà Nội") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Đăng bán sản phẩm", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(Icons.Outlined.Close, contentDescription = "Đóng")
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tên sản phẩm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit) },
                    label = { Text("Giá bán (VNĐ, để trống = Thỏa thuận)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MARKET_CATEGORIES.drop(1)) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Tình trạng") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Khu vực") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Mô tả chi tiết") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Ảnh sản phẩm",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chọn ảnh")
                    }
                    Button(
                        onClick = {
                            onPublish(title, priceText.toLongOrNull() ?: 0L, category, condition, location, description, imageUri)
                        },
                        enabled = !isUploading && title.trim().length >= 2
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Đăng bán")
                    }
                }
            }
        }
    }
}

/**
 * Học Tiếng Trung · Tiếng Anh Screen matching web learn.html:
 * - Language switcher: Tiếng Trung (zh) / Tiếng Anh (en)
 * - 3 modes: Thẻ từ vựng (Flashcards + TTS + Add Card), Trắc nghiệm (Quiz), Công cụ (Pomodoro + Sổ ghi chú)
 */
@Composable
fun LearnScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var lang by remember { mutableStateOf("zh") }
    var mode by remember { mutableIntStateOf(0) } // 0=flashcards, 1=quiz, 2=toolkit
    var cards by remember { mutableStateOf(DEFAULT_FLASHCARDS) }
    var masteredIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAddCard by remember { mutableStateOf(false) }

    // On-device Android TextToSpeech for real audio pronunciation
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
        engine
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speak(text: String, language: String) {
        if (!ttsReady) return
        val locale = if (language == "zh") Locale.SIMPLIFIED_CHINESE else Locale.US
        tts.language = locale
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "twin_learn_tts")
    }

    val langCards = remember(cards, lang) { cards.filter { it.lang == lang } }

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
            item(key = "learn_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Học Tiếng Trung · Tiếng Anh",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Flashcards phát âm, Trắc nghiệm & Pomodoro",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showAddCard = true }, modifier = Modifier.testTag("learn_add_card_btn")) {
                            Icon(Icons.Filled.Add, contentDescription = "Thêm thẻ từ vựng")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = lang == "zh",
                            onClick = { lang = "zh" },
                            label = { Text("🇨🇳 Tiếng Trung") }
                        )
                        FilterChip(
                            selected = lang == "en",
                            onClick = { lang = "en" },
                            label = { Text("🇬🇧 Tiếng Anh") }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Thẻ từ vựng") })
                        FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Trắc nghiệm") })
                        FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("Pomodoro & Ghi chú") })
                    }
                }
            }

            item(key = "learn_body") {
                when (mode) {
                    0 -> FlashcardsSection(
                        cards = langCards,
                        masteredIds = masteredIds,
                        onToggleMastered = { id ->
                            masteredIds = if (id in masteredIds) masteredIds - id else masteredIds + id
                        },
                        onSpeak = { speak(it, lang) }
                    )

                    1 -> QuizSection(cards = langCards, onSpeak = { speak(it, lang) })

                    else -> StudyToolkitSection()
                }
            }
        }
    }

    if (showAddCard) {
        var front by remember { mutableStateOf("") }
        var phonetic by remember { mutableStateOf("") }
        var meaning by remember { mutableStateOf("") }
        var example by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCard = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Thêm thẻ từ vựng mới", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(value = front, onValueChange = { front = it }, label = { Text("Từ vựng") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phonetic, onValueChange = { phonetic = it }, label = { Text("Phiên âm (Pinyin / IPA)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("Nghĩa tiếng Việt") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = example, onValueChange = { example = it }, label = { Text("Câu ví dụ") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showAddCard = false }) { Text("Hủy") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                cards = listOf(
                                    FlashcardItem(
                                        id = "custom_${System.currentTimeMillis()}",
                                        lang = lang,
                                        front = front.trim(),
                                        phonetic = phonetic.trim(),
                                        meaning = meaning.trim(),
                                        example = example.trim()
                                    )
                                ) + cards
                                showAddCard = false
                            },
                            enabled = front.isNotBlank() && meaning.isNotBlank()
                        ) {
                            Text("Lưu thẻ")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardsSection(
    cards: List<FlashcardItem>,
    masteredIds: Set<String>,
    onToggleMastered: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    if (cards.isEmpty()) {
        FeedStatusCard("Chưa có thẻ từ vựng", "Nhấn dấu + ở góc trên để thêm thẻ mới.")
        return
    }
    var index by remember(cards) { mutableIntStateOf(0) }
    var flipped by remember(cards, index) { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, cards.lastIndex)
    val card = cards[safeIndex]
    val masteredCount = cards.count { it.id in masteredIds }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Thẻ ${safeIndex + 1} / ${cards.size}", style = MaterialTheme.typography.labelMedium)
            Text("Đã thuộc: $masteredCount / ${cards.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { masteredCount.toFloat() / cards.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clickable { flipped = !flipped },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                IconButton(
                    onClick = { onSpeak(card.front) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Nghe phát âm", tint = MaterialTheme.colorScheme.primary)
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!flipped) {
                        Text(
                            text = card.front,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.Center
                        )
                        if (card.phonetic.isNotBlank()) {
                            Text(
                                text = card.phonetic,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Chạm vào thẻ để lật xem nghĩa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = card.meaning,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (card.example.isNotBlank()) {
                            Text(
                                text = card.example,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { index = if (safeIndex > 0) safeIndex - 1 else cards.lastIndex }
            ) {
                Text("Trước")
            }
            FilterChip(
                selected = card.id in masteredIds,
                onClick = { onToggleMastered(card.id) },
                label = { Text(if (card.id in masteredIds) "Đã thuộc" else "Đánh dấu thuộc") },
                leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Button(
                onClick = { index = (safeIndex + 1) % cards.size }
            ) {
                Text("Tiếp theo")
            }
        }
    }
}

@Composable
private fun QuizSection(cards: List<FlashcardItem>, onSpeak: (String) -> Unit) {
    if (cards.size < 2) {
        FeedStatusCard("Cần ít nhất 2 từ vựng để làm Trắc nghiệm", "Hãy thêm thêm thẻ từ vựng.")
        return
    }
    var questionIdx by remember(cards) { mutableIntStateOf(0) }
    var selectedAnswer by remember(questionIdx, cards) { mutableStateOf<String?>(null) }
    var score by remember(cards) { mutableIntStateOf(0) }

    val current = cards[questionIdx % cards.size]
    val options = remember(current, cards) {
        (listOf(current.meaning) + cards.filter { it.id != current.id }.map { it.meaning }.shuffled().take(3)).shuffled()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Câu hỏi ${(questionIdx % cards.size) + 1}/${cards.size}", style = MaterialTheme.typography.labelLarge)
                Text("Điểm: $score", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(current.front, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                IconButton(onClick = { onSpeak(current.front) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Nghe phát âm", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(current.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            options.forEach { opt ->
                val isCorrect = opt == current.meaning
                val isPicked = selectedAnswer == opt
                val containerColor = when {
                    selectedAnswer == null -> MaterialTheme.colorScheme.surfaceContainerLow
                    isCorrect -> Color(0xFF16A34A).copy(alpha = 0.2f)
                    isPicked -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = selectedAnswer == null) {
                            selectedAnswer = opt
                            if (isCorrect) score++
                        }
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }

            if (selectedAnswer != null) {
                Button(
                    onClick = { questionIdx++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Câu tiếp theo")
                }
            }
        }
    }
}

@Composable
private fun StudyToolkitSection() {
    var focusMode by remember { mutableStateOf(true) } // true = 25m, false = 5m
    var secondsLeft by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }

    var noteDraft by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(running) {
        while (running && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        if (secondsLeft == 0) running = false
    }

    val mm = (secondsLeft / 60).toString().padStart(2, '0')
    val ss = (secondsLeft % 60).toString().padStart(2, '0')

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Pomodoro Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("⏱ Đồng hồ Pomodoro tập trung", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = focusMode,
                        onClick = { focusMode = true; running = false; secondsLeft = 25 * 60 },
                        label = { Text("Tập trung 25p") }
                    )
                    FilterChip(
                        selected = !focusMode,
                        onClick = { focusMode = false; running = false; secondsLeft = 5 * 60 },
                        label = { Text("Nghỉ ngắn 5p") }
                    )
                }
                Text(
                    text = "$mm:$ss",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { running = !running }) {
                        Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (running) "Tạm dừng" else "Bắt đầu")
                    }
                    OutlinedButton(onClick = { running = false; secondsLeft = if (focusMode) 25 * 60 else 5 * 60 }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đặt lại")
                    }
                }
            }
        }

        // Study Notes Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📝 Sổ ghi chú học tập", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        placeholder = { Text("Ghi nhanh ngữ pháp hoặc từ mới...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (noteDraft.isNotBlank()) {
                                notes = listOf(noteDraft.trim()) + notes
                                noteDraft = ""
                            }
                        },
                        enabled = noteDraft.isNotBlank()
                    ) {
                        Text("Lưu")
                    }
                }
                if (notes.isEmpty()) {
                    Text("Chưa có ghi chú nào trong phiên học này.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    notes.forEachIndexed { idx, note ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { notes = notes.filterIndexed { i, _ -> i != idx } }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa ghi chú", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Trợ giúp & Hướng dẫn sử dụng Twin Screen matching web help.html
 */
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val guides = remember {
        listOf(
            "Đồng bộ trực tiếp với web Twin (ndtwws99.github.io)" to
                "Ứng dụng Android Twin kết nối thẳng vào cùng cơ sở dữ liệu Firebase của bản web trên dự án ndtwws99-1999. Mọi bài viết, ảnh, tệp, Tin 24h, sản phẩm Chợ Twin và tin nhắn Twin Chat đều đồng bộ hai chiều theo thời gian thực.",
            "Thanh bên phải mở rộng & Điều hướng" to
                "Nhấn vào ảnh đại diện ở góc trên bên phải của thanh tiêu đề để mở Thanh bên phải mở rộng: xem Tổng quan Twin, truy cập nhanh Bạn bè, Nhóm, Trang, Chợ Twin, Học Trung · Anh, Bài đã lưu, đổi chế độ Sáng/Tối và nhắn tin nhanh với Người liên hệ đang hoạt động.",
            "Đăng bài viết, ảnh & tệp đính kèm" to
                "Trên Bảng tin hoặc Twin Chat, bạn có thể chọn ảnh từ thư viện hoặc đính kèm tệp tài liệu để tải lên Cloudinary và chia sẻ ngay lập tức.",
            "Chế độ Khách & Tài khoản" to
                "Bạn có thể đăng nhập bằng Email/Mật khẩu, Google hoặc dùng thử Chế độ Khách cục bộ. Đăng nhập tài khoản thành viên để đồng bộ đầy đủ dữ liệu đám mây."
        )
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
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Trợ giúp & Hướng dẫn Twin",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(guides) { (title, body) ->
                var expanded by remember { mutableStateOf(true) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
