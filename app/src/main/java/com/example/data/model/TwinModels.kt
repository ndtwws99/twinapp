package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TwinReaction(val key: String, val emoji: String, val label: String) {
    LIKE("like", "👍", "Thích"),
    LOVE("love", "❤️", "Yêu thích"),
    HAHA("haha", "😂", "Haha"),
    WOW("wow", "😮", "Wow"),
    SAD("sad", "😢", "Buồn"),
    ANGRY("angry", "😡", "Phẫn nộ");

    companion object {
        fun fromKey(raw: String?): TwinReaction? =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) }
    }
}

@Keep
@IgnoreExtraProperties
data class TwinUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "Thành viên Twin",
    val email: String = "",
    val role: String = "member",
    val avatar: String = "",
    val cover: String = "",
    val bio: String = "Xin chào, mình đang dùng Twin!",
    val location: String = "",
    val work: String = "",
    val website: String = "",
    val discoverable: Boolean = true,
    val online: Boolean = false,
    @get:PropertyName("isOnline")
    @PropertyName("isOnline")
    val webOnline: Boolean = false,
    val showActivity: Boolean = true,
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(),
    val savedPosts: List<String> = emptyList(),
    val lastActiveAt: Any? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveUid: String
        get() = uid.ifBlank { id }

    val lastActiveMillis: Long
        get() = when (val raw = lastActiveAt) {
            is Number -> raw.toLong()
            is Timestamp -> raw.toDate().time
            else -> 0L
        }

    val isCurrentlyOnline: Boolean
        get() {
            if (!showActivity) return false
            val activeRecently = lastActiveMillis > 0L && (System.currentTimeMillis() - lastActiveMillis) <= 180_000L
            return online || webOnline || activeRecently
        }
}

@Keep
@IgnoreExtraProperties
data class TwinComment(
    val commentId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to commentId,
        "commentId" to commentId,
        "authorUid" to authorUid,
        "authorName" to authorName,
        "authorAvatar" to authorAvatar,
        "text" to text,
        "createdAt" to createdAtMillis,
        "createdAtMillis" to createdAtMillis
    )
}

@Keep
@IgnoreExtraProperties
data class TwinPost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val feeling: String = "",
    val privacy: String = "public",
    val allowedFriendUids: List<String> = emptyList(),
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val videoUrl: String = "",
    val videoPoster: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val pageId: String = "",
    val pageName: String = "",
    val postType: String = "",
    val marketTitle: String = "",
    val marketPrice: Long = 0L,
    val marketCategory: String = "",
    val marketCondition: String = "",
    val marketLocation: String = "",
    val marketStatus: String = "available",
    val pinned: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
    val comments: List<Map<String, Any>> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val isMarketPost: Boolean
        get() = postType.equals("market", ignoreCase = true) || pageId == "twin_market"

    val allImageUrls: List<String>
        get() = buildList {
            imageUrls.forEach { if (it.isNotBlank()) add(it) }
            images.forEach { if (it.isNotBlank() && it !in this) add(it) }
            if (imageUrl.isNotBlank() && imageUrl !in this) add(imageUrl)
        }

    val primaryImageUrl: String
        get() = allImageUrls.firstOrNull().orEmpty()

    fun parsedComments(): List<TwinComment> = comments.map { raw ->
        val rawCreated = raw["createdAtMillis"] ?: raw["createdAt"]
        val millis = when (rawCreated) {
            is Number -> rawCreated.toLong()
            is Timestamp -> rawCreated.toDate().time
            else -> 0L
        }
        TwinComment(
            commentId = (raw["commentId"] ?: raw["id"])?.toString().orEmpty(),
            authorUid = raw["authorUid"]?.toString().orEmpty(),
            authorName = raw["authorName"]?.toString().orEmpty().ifBlank { "Thành viên Twin" },
            authorAvatar = raw["authorAvatar"]?.toString().orEmpty(),
            text = raw["text"]?.toString().orEmpty(),
            createdAtMillis = millis
        )
    }
}

@Keep
@IgnoreExtraProperties
data class TwinStory(
    @DocumentId val id: String = "",
    val type: String = "text",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val text: String = "",
    val bgId: String = "ig",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val caption: String = "",
    val privacy: String = "public",
    val likes: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TwinGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val privacy: String = "public",
    val coverUrl: String = "",
    val ownerUid: String = "",
    val chatId: String = "",
    @get:PropertyName("isChatGroup")
    @PropertyName("isChatGroup")
    val chatGroup: Boolean = false,
    val members: List<String> = emptyList(),
    val pendingMembers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TwinPage(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Cộng đồng",
    val description: String = "",
    val coverUrl: String = "",
    val visibility: String = "public",
    val ownerUid: String = "",
    val followers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TwinNotification(
    @DocumentId val id: String = "",
    val toUid: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    val type: String = "reaction",
    val text: String = "",
    val message: String = "",
    val link: String = "",
    val postId: String = "",
    val storyId: String = "",
    val read: Boolean = false,
    val createdAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val displayBody: String
        get() = text.ifBlank { message }

    val isMessageNotification: Boolean
        get() = type == "message" || link.startsWith("./messages.html")
}

@Keep
@IgnoreExtraProperties
data class TwinChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val text: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun buildChatId(uid1: String, uid2: String): String =
    listOf(uid1, uid2).sorted().joinToString("_")

fun formatVndPrice(price: Long): String {
    if (price <= 0L) return "Thỏa thuận / Tặng"
    val raw = price.toString()
    val sb = StringBuilder()
    for (i in raw.indices) {
        if (i > 0 && (raw.length - i) % 3 == 0) sb.append('.')
        sb.append(raw[i])
    }
    return "$sb ₫"
}
