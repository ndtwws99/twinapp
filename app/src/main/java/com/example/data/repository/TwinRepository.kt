package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.TwinComment
import com.example.data.model.TwinNotification
import com.example.data.model.TwinPost
import com.example.data.model.TwinStory
import com.example.data.model.TwinUser
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TwinRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập để sử dụng Twin.")

    suspend fun ensureCurrentUserProfile(): Result<TwinUser> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val uid = currentUser.uid
        val path = "users/$uid"
        return try {
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            val displayName = currentUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Thành viên Twin"
            val photoUrl = currentUser.photoUrl?.toString().orEmpty()
            val email = currentUser.email.orEmpty()

            if (!snap.exists()) {
                val payload = mapOf(
                    "uid" to uid,
                    "name" to displayName.take(80),
                    "email" to email.take(254),
                    "role" to "member",
                    "avatar" to photoUrl.take(1000),
                    "cover" to "",
                    "bio" to "Xin chào, mình đang dùng Twin!",
                    "location" to "",
                    "work" to "",
                    "website" to "",
                    "discoverable" to true,
                    "online" to true,
                    "isOnline" to true,
                    "showActivity" to true,
                    "friends" to emptyList<String>(),
                    "friendRequests" to emptyList<String>(),
                    "savedPosts" to emptyList<String>(),
                    "lastActiveAt" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            } else {
                ref.update(
                    mapOf(
                        "online" to true,
                        "isOnline" to true,
                        "lastActiveAt" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val updatedSnap = ref.get().await()
            val user = updatedSnap.toObject(TwinUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: TwinUser(id = uid, uid = uid, name = displayName, avatar = photoUrl, email = email)
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markOfflineBeforeSignOut() {
        val uid = auth.currentUser?.uid ?: return
        val path = "users/$uid"
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "online" to false,
                    "isOnline" to false,
                    "lastActiveAt" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    fun observeCurrentUser(uid: String): Flow<TwinUser?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(TwinUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    /**
     * Observes all Twin users matching web `collection(db, "users")` (does not exclude legacy web docs without `discoverable`).
     */
    fun observeDiscoverableUsers(uid: String): Flow<List<TwinUser>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TwinUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.role != "guest" }
                        .sortedByDescending { it.isCurrentlyOnline }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * Observes Twin posts & market listings matching web `collection(db, "posts")` in real time.
     */
    fun observePublicPosts(uid: String): Flow<List<TwinPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TwinPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { post ->
                            post.privacy != "friends" ||
                                post.authorUid == uid ||
                                uid in post.allowedFriendUids
                        }
                        .sortedWith(
                            compareByDescending<TwinPost> { it.pinned }
                                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePublicStories(uid: String): Flow<List<TwinStory>> = flow {
        val path = "stories"
        emitAll(
            db.collection("stories")
                .snapshots()
                .map { snap ->
                    val cutoffMs = System.currentTimeMillis() - 48L * 3600L * 1000L
                    snap.toObjects(TwinStory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { story ->
                            val createdMs = story.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                            (story.privacy != "private" || story.authorUid == uid) && createdMs >= cutoffMs
                        }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeNotifications(uid: String): Flow<List<TwinNotification>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("toUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TwinNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { notif ->
                            notif.createdAt?.toDate()?.time ?: notif.createdAtMs
                        }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createPost(
        author: TwinUser,
        content: String,
        feeling: String,
        privacy: String,
        imageUrl: String,
        fileUrl: String = "",
        fileName: String = "",
        groupId: String = "",
        groupName: String = ""
    ): Result<Unit> {
        val path = "posts"
        return try {
            val uid = requireUserId()
            val trimmedContent = content.trim().take(5000)
            val trimmedImg = imageUrl.trim().take(1500)
            val trimmedFile = fileUrl.trim().take(1500)
            require(trimmedContent.isNotEmpty() || trimmedImg.isNotEmpty() || trimmedFile.isNotEmpty()) {
                "Hãy nhập nội dung hoặc đính kèm ảnh/tệp trước khi đăng."
            }
            val safePrivacy = if (privacy == "friends") "friends" else "public"
            val payload = mutableMapOf<String, Any>(
                "authorUid" to uid,
                "authorName" to author.name.ifBlank { "Thành viên Twin" }.take(80),
                "authorAvatar" to author.avatar.take(1000),
                "content" to trimmedContent,
                "feeling" to feeling.trim().take(80),
                "privacy" to safePrivacy,
                "allowedFriendUids" to if (safePrivacy == "friends") author.friends.take(500) else emptyList<String>(),
                "imageUrl" to trimmedImg,
                "imageUrls" to if (trimmedImg.isNotBlank()) listOf(trimmedImg) else emptyList<String>(),
                "pinned" to false,
                "reactions" to emptyMap<String, String>(),
                "comments" to emptyList<Map<String, Any>>(),
                "savedBy" to emptyList<String>(),
                "sharesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (trimmedFile.isNotBlank()) {
                payload["fileUrl"] = trimmedFile
                payload["fileName"] = fileName.trim().take(200)
            }
            if (groupId.isNotBlank()) {
                payload["groupId"] = groupId.take(128)
                payload["groupName"] = groupName.take(120)
            }
            db.collection("posts").add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Publishes a Chợ Twin product listing matching web market.html exact Firestore schema.
     */
    suspend fun createMarketProduct(
        author: TwinUser,
        title: String,
        priceVnd: Long,
        category: String,
        condition: String,
        location: String,
        description: String,
        imageUrl: String
    ): Result<Unit> {
        val path = "posts"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2) { "Tên sản phẩm phải có ít nhất 2 ký tự." }
            val trimmedImg = imageUrl.trim().take(1500)
            val payload = mapOf(
                "postType" to "market",
                "marketTitle" to cleanTitle,
                "marketPrice" to priceVnd.coerceAtLeast(0L),
                "marketCategory" to category.ifBlank { "Khác" }.take(80),
                "marketCondition" to condition.ifBlank { "Mới" }.take(60),
                "marketLocation" to location.trim().take(120),
                "marketStatus" to "available",
                "content" to description.trim().take(3000),
                "imageUrl" to trimmedImg,
                "images" to if (trimmedImg.isNotBlank()) listOf(trimmedImg) else emptyList<String>(),
                "authorUid" to uid,
                "authorName" to author.name.ifBlank { "Thành viên Twin" }.take(80),
                "authorAvatar" to author.avatar.take(1000),
                "privacy" to "public",
                "pinned" to false,
                "reactions" to emptyMap<String, String>(),
                "comments" to emptyList<Map<String, Any>>(),
                "savedBy" to emptyList<String>(),
                "sharesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostReaction(post: TwinPost, reactionKey: String, actor: TwinUser): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val updated = post.reactions.toMutableMap()
            val isRemoving = updated[uid] == reactionKey
            if (isRemoving) updated.remove(uid) else updated[uid] = reactionKey
            db.collection("posts").document(post.id)
                .update("reactions", updated)
                .await()
            if (!isRemoving && post.authorUid.isNotBlank() && post.authorUid != uid) {
                pushNotification(
                    toUid = post.authorUid,
                    actor = actor,
                    type = "reaction",
                    text = "đã thả cảm xúc vào bài viết của bạn.",
                    postId = post.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(post: TwinPost, text: String, actor: TwinUser): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val trimmed = text.trim().take(1000)
            require(trimmed.isNotEmpty()) { "Nội dung bình luận không được để trống." }
            val comment = TwinComment(
                commentId = UUID.randomUUID().toString(),
                authorUid = uid,
                authorName = actor.name.ifBlank { "Thành viên Twin" }.take(80),
                authorAvatar = actor.avatar.take(1000),
                text = trimmed,
                createdAtMillis = System.currentTimeMillis()
            )
            db.collection("posts").document(post.id)
                .update("comments", FieldValue.arrayUnion(comment.toMap()))
                .await()
            if (post.authorUid.isNotBlank() && post.authorUid != uid) {
                pushNotification(
                    toUid = post.authorUid,
                    actor = actor,
                    type = "comment",
                    text = "đã bình luận: \"${trimmed.take(45)}\"",
                    postId = post.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(post: TwinPost): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val alreadySaved = uid in post.savedBy
            val postOp = if (alreadySaved) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("posts").document(post.id).update("savedBy", postOp).await()
            val userOp = if (alreadySaved) FieldValue.arrayRemove(post.id) else FieldValue.arrayUnion(post.id)
            db.collection("users").document(uid).update("savedPosts", userOp).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementShareCount(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.collection("posts").document(postId)
                .update("sharesCount", FieldValue.increment(1L))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createStory(
        author: TwinUser,
        type: String,
        text: String,
        bgId: String,
        imageUrl: String,
        caption: String
    ): Result<Unit> {
        val path = "stories"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "type" to if (type == "image") "image" else "text",
                "authorUid" to uid,
                "authorName" to author.name.ifBlank { "Thành viên Twin" }.take(80),
                "authorAvatar" to author.avatar.take(1000),
                "text" to text.trim().take(500),
                "bgId" to bgId.ifBlank { "ig" }.take(40),
                "imageUrl" to imageUrl.trim().take(1500),
                "caption" to caption.trim().take(300),
                "privacy" to "public",
                "likes" to emptyList<String>(),
                "savedBy" to emptyList<String>(),
                "replies" to emptyList<Map<String, Any>>(),
                "sharesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("stories").add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleStoryLike(story: TwinStory): Result<Unit> {
        val path = "stories/${story.id}"
        return try {
            val uid = requireUserId()
            val op = if (uid in story.likes) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("stories").document(story.id).update("likes", op).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        name: String,
        bio: String,
        location: String,
        work: String,
        website: String,
        avatar: String,
        cover: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val path = "users/$uid"
        return try {
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Tên hiển thị không được để trống." }
            db.collection("users").document(uid).update(
                mapOf(
                    "name" to cleanName,
                    "bio" to bio.trim().take(400),
                    "location" to location.trim().take(120),
                    "work" to work.trim().take(120),
                    "website" to website.trim().take(300),
                    "avatar" to avatar.trim().take(1000),
                    "cover" to cover.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val path = "notifications/$notifId"
        return try {
            requireUserId()
            db.collection("notifications").document(notifId).update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(notifs: List<TwinNotification>): Result<Unit> {
        val path = "notifications"
        return try {
            requireUserId()
            val unread = notifs.filter { !it.read }
            if (unread.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unread.take(100).forEach { n ->
                batch.update(db.collection("notifications").document(n.id), "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    internal suspend fun pushNotification(
        toUid: String,
        actor: TwinUser,
        type: String,
        text: String,
        postId: String = "",
        link: String = ""
    ) {
        val fromUid = auth.currentUser?.uid ?: return
        if (toUid.isBlank() || toUid == fromUid) return
        val path = "notifications"
        try {
            val payload = mutableMapOf<String, Any>(
                "toUid" to toUid,
                "fromUid" to fromUid,
                "fromName" to actor.name.ifBlank { "Thành viên Twin" }.take(80),
                "fromAvatar" to actor.avatar.take(1000),
                "type" to type,
                "text" to text.take(300),
                "message" to text.take(300),
                "link" to link,
                "read" to false,
                "createdAtMs" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (postId.isNotBlank()) payload["postId"] = postId
            db.collection("notifications").add(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
