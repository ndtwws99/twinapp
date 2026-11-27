package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.TwinChatMessage
import com.example.data.model.TwinGroup
import com.example.data.model.TwinPage
import com.example.data.model.TwinUser
import com.example.data.model.buildChatId
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

class TwinCommunityRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập để sử dụng Twin.")

    /**
     * Observes community groups matching web groups.html (excluding private chat groups).
     */
    fun observePublicGroups(uid: String): Flow<List<TwinGroup>> = flow {
        val path = "groups"
        emitAll(
            db.collection("groups")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TwinGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { !it.chatGroup }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * Observes all pages matching web pages.html (no `visibility` filter that hides web pages).
     */
    fun observePublicPages(uid: String): Flow<List<TwinPage>> = flow {
        val path = "pages"
        emitAll(
            db.collection("pages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TwinPage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * Observes 1-on-1 Twin Chat messages matching web messages.html path `messages/{chatId}/items`.
     */
    fun observeChatMessages(myUid: String, otherUid: String): Flow<List<TwinChatMessage>> = flow {
        val chatId = buildChatId(myUid, otherUid)
        val path = "messages/$chatId/items"
        emitAll(
            db.collection("messages")
                .document(chatId)
                .collection("items")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TwinChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, myUid)
                    throw error
                }
        )
    }

    suspend fun sendFriendRequest(targetUid: String, actor: TwinUser, mainRepo: TwinRepository): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            val myUid = requireUserId()
            require(targetUid.isNotBlank() && targetUid != myUid)
            db.collection("users").document(targetUid)
                .update("friendRequests", FieldValue.arrayUnion(myUid))
                .await()
            mainRepo.pushNotification(
                toUid = targetUid,
                actor = actor,
                type = "friend_request",
                text = "đã gửi cho bạn lời mời kết bạn.",
                link = "./friends.html"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(targetUid: String): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            val myUid = requireUserId()
            db.collection("users").document(targetUid)
                .update("friendRequests", FieldValue.arrayRemove(myUid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requesterUid: String, actor: TwinUser, mainRepo: TwinRepository): Result<Unit> {
        val myUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val path = "users/$myUid"
        return try {
            val batch = db.batch()
            val myRef = db.collection("users").document(myUid)
            val otherRef = db.collection("users").document(requesterUid)
            batch.update(
                myRef,
                mapOf(
                    "friends" to FieldValue.arrayUnion(requesterUid),
                    "friendRequests" to FieldValue.arrayRemove(requesterUid)
                )
            )
            batch.update(otherRef, "friends", FieldValue.arrayUnion(myUid))
            batch.commit().await()
            mainRepo.pushNotification(
                toUid = requesterUid,
                actor = actor,
                type = "friend_accept",
                text = "đã chấp nhận lời mời kết bạn của bạn!",
                link = "./profile.html?uid=$myUid"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun declineFriendRequest(requesterUid: String): Result<Unit> {
        val myUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val path = "users/$myUid"
        return try {
            db.collection("users").document(myUid)
                .update("friendRequests", FieldValue.arrayRemove(requesterUid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun unfriend(targetUid: String): Result<Unit> {
        val myUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val path = "users/$myUid"
        return try {
            val batch = db.batch()
            batch.update(db.collection("users").document(myUid), "friends", FieldValue.arrayRemove(targetUid))
            batch.update(db.collection("users").document(targetUid), "friends", FieldValue.arrayRemove(myUid))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, description: String): Result<Unit> {
        val path = "groups"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(100)
            require(cleanName.length >= 2) { "Tên nhóm phải có ít nhất 2 ký tự." }
            db.collection("groups").add(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(600),
                    "privacy" to "public",
                    "coverUrl" to "",
                    "ownerUid" to uid,
                    "members" to listOf(uid),
                    "pendingMembers" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleJoinGroup(group: TwinGroup): Result<Unit> {
        val path = "groups/${group.id}"
        return try {
            val uid = requireUserId()
            val op = if (uid in group.members) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("groups").document(group.id).update("members", op).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPage(name: String, category: String, description: String): Result<Unit> {
        val path = "pages"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(100)
            require(cleanName.length >= 2) { "Tên Trang phải có ít nhất 2 ký tự." }
            db.collection("pages").add(
                mapOf(
                    "name" to cleanName,
                    "category" to category.trim().ifBlank { "Cộng đồng" }.take(80),
                    "description" to description.trim().take(600),
                    "coverUrl" to "",
                    "ownerUid" to uid,
                    "followers" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollowPage(page: TwinPage): Result<Unit> {
        val path = "pages/${page.id}"
        return try {
            val uid = requireUserId()
            val op = if (uid in page.followers) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("pages").document(page.id).update("followers", op).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        otherUid: String,
        text: String,
        imageUrl: String,
        fileUrl: String = "",
        fileName: String = "",
        sender: TwinUser,
        mainRepo: TwinRepository
    ): Result<Unit> {
        val myUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val chatId = buildChatId(myUid, otherUid)
        val path = "messages/$chatId/items"
        return try {
            val trimmedText = text.trim().take(2000)
            val trimmedImg = imageUrl.trim().take(1500)
            val trimmedFile = fileUrl.trim().take(1500)
            require(trimmedText.isNotEmpty() || trimmedImg.isNotEmpty() || trimmedFile.isNotEmpty()) {
                "Nhập tin nhắn hoặc đính kèm ảnh/tệp."
            }
            val payload = mutableMapOf<String, Any>(
                "senderUid" to myUid,
                "senderName" to sender.name.ifBlank { "Thành viên Twin" }.take(80),
                "memberUids" to listOf(myUid, otherUid).sorted(),
                "text" to trimmedText,
                "imageUrl" to trimmedImg,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (trimmedFile.isNotBlank()) {
                payload["fileUrl"] = trimmedFile
                payload["fileName"] = fileName.trim().take(200)
            }
            db.collection("messages")
                .document(chatId)
                .collection("items")
                .add(payload)
                .await()

            val preview = when {
                trimmedText.isNotBlank() -> "đã nhắn: \"${trimmedText.take(45)}\""
                trimmedImg.isNotBlank() -> "đã gửi một hình ảnh."
                else -> "đã gửi tệp ${fileName.take(40)}"
            }
            mainRepo.pushNotification(
                toUid = otherUid,
                actor = sender,
                type = "message",
                text = preview,
                link = "./messages.html?uid=$myUid"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatMessage(otherUid: String, messageId: String): Result<Unit> {
        val myUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Chưa đăng nhập."))
        val chatId = buildChatId(myUid, otherUid)
        val path = "messages/$chatId/items/$messageId"
        return try {
            db.collection("messages")
                .document(chatId)
                .collection("items")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
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
