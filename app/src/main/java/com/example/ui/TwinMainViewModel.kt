package com.example.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.TwinChatMessage
import com.example.data.model.TwinGroup
import com.example.data.model.TwinNotification
import com.example.data.model.TwinPage
import com.example.data.model.TwinPost
import com.example.data.model.TwinStory
import com.example.data.model.TwinUser
import com.example.data.model.UiState
import com.example.data.remote.CloudinaryUploader
import com.example.data.repository.TwinCommunityRepository
import com.example.data.repository.TwinRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TwinMainViewModel(
    val currentUserId: String,
    private val repository: TwinRepository,
    private val communityRepo: TwinCommunityRepository,
    private val uploader: CloudinaryUploader
) : ViewModel() {

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isUploadingMedia = MutableStateFlow(false)
    val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

    val currentUser: StateFlow<TwinUser?> = repository.observeCurrentUser(currentUserId)
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing current user", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    val usersState: StateFlow<UiState<List<TwinUser>>> =
        repository.observeDiscoverableUsers(currentUserId).toUiState("Không thể tải danh sách thành viên")

    val postsState: StateFlow<UiState<List<TwinPost>>> =
        repository.observePublicPosts(currentUserId).toUiState("Không thể tải bảng tin")

    val storiesState: StateFlow<UiState<List<TwinStory>>> =
        repository.observePublicStories(currentUserId).toUiState("Không thể tải tin 24h")

    val groupsState: StateFlow<UiState<List<TwinGroup>>> =
        communityRepo.observePublicGroups(currentUserId).toUiState("Không thể tải danh sách nhóm")

    val pagesState: StateFlow<UiState<List<TwinPage>>> =
        communityRepo.observePublicPages(currentUserId).toUiState("Không thể tải danh sách trang")

    val notificationsState: StateFlow<UiState<List<TwinNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("Không thể tải thông báo")

    init {
        viewModelScope.launch {
            val res = repository.ensureCurrentUserProfile()
            res.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Không thể đồng bộ hồ sơ người dùng."
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun observeChatWith(otherUid: String): Flow<UiState<List<TwinChatMessage>>> =
        communityRepo.observeChatMessages(currentUserId, otherUid)
            .map<List<TwinChatMessage>, UiState<List<TwinChatMessage>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing chat messages", root)
                emit(UiState.Error(root.message ?: "Không thể tải tin nhắn"))
            }

    private fun activeActor(): TwinUser =
        currentUser.value ?: TwinUser(id = currentUserId, uid = currentUserId, name = "Thành viên Twin")

    fun createPost(
        content: String,
        feeling: String,
        privacy: String,
        imageUri: Uri?,
        fileUri: Uri? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            var uploadedImgUrl = ""
            var uploadedFileUrl = ""
            var uploadedFileName = ""

            if (imageUri != null) {
                _isUploadingMedia.value = true
                val uploadRes = uploader.uploadImage(imageUri)
                _isUploadingMedia.value = false
                if (uploadRes.isFailure) {
                    _snackbarMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "Tải ảnh thất bại."
                    return@launch
                }
                uploadedImgUrl = uploadRes.getOrDefault("")
            }
            if (fileUri != null) {
                _isUploadingMedia.value = true
                val uploadRes = uploader.uploadAnyFile(fileUri)
                _isUploadingMedia.value = false
                if (uploadRes.isFailure) {
                    _snackbarMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "Tải tệp thất bại."
                    return@launch
                }
                val asset = uploadRes.getOrNull()
                if (asset != null) {
                    uploadedFileUrl = asset.url
                    uploadedFileName = asset.fileName
                }
            }
            val res = repository.createPost(
                author = activeActor(),
                content = content,
                feeling = feeling,
                privacy = privacy,
                imageUrl = uploadedImgUrl,
                fileUrl = uploadedFileUrl,
                fileName = uploadedFileName
            )
            res.onSuccess {
                _snackbarMessage.value = "Đã đăng bài viết lên Twin!"
                onDone()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Không thể đăng bài viết."
            }
        }
    }

    fun createMarketProduct(
        title: String,
        priceVnd: Long,
        category: String,
        condition: String,
        location: String,
        description: String,
        imageUri: Uri?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            var uploadedUrl = ""
            if (imageUri != null) {
                _isUploadingMedia.value = true
                val uploadRes = uploader.uploadImage(imageUri)
                _isUploadingMedia.value = false
                if (uploadRes.isFailure) {
                    _snackbarMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "Tải ảnh sản phẩm thất bại."
                    return@launch
                }
                uploadedUrl = uploadRes.getOrDefault("")
            }
            repository.createMarketProduct(
                author = activeActor(),
                title = title,
                priceVnd = priceVnd,
                category = category,
                condition = condition,
                location = location,
                description = description,
                imageUrl = uploadedUrl
            ).onSuccess {
                _snackbarMessage.value = "Đã đăng bán sản phẩm lên Chợ Twin!"
                onDone()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Không thể đăng bán sản phẩm."
            }
        }
    }

    fun toggleReaction(post: TwinPost, reactionKey: String) {
        viewModelScope.launch {
            repository.togglePostReaction(post, reactionKey, activeActor())
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Lỗi thả cảm xúc." }
        }
    }

    fun addComment(post: TwinPost, text: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addComment(post, text, activeActor())
                .onSuccess { onSuccess() }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Lỗi gửi bình luận." }
        }
    }

    fun toggleSavePost(post: TwinPost) {
        viewModelScope.launch {
            repository.toggleSavePost(post)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể lưu bài viết." }
        }
    }

    fun recordShare(postId: String) {
        viewModelScope.launch {
            repository.incrementShareCount(postId)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess { _snackbarMessage.value = "Đã xóa bài viết." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể xóa bài viết." }
        }
    }

    fun createStory(
        text: String,
        bgId: String,
        imageUri: Uri?,
        caption: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            var uploadedUrl = ""
            if (imageUri != null) {
                _isUploadingMedia.value = true
                val uploadRes = uploader.uploadImage(imageUri)
                _isUploadingMedia.value = false
                if (uploadRes.isFailure) {
                    _snackbarMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "Tải ảnh tin thất bại."
                    return@launch
                }
                uploadedUrl = uploadRes.getOrDefault("")
            }
            val type = if (uploadedUrl.isNotBlank()) "image" else "text"
            repository.createStory(
                author = activeActor(),
                type = type,
                text = text,
                bgId = bgId,
                imageUrl = uploadedUrl,
                caption = caption
            ).onSuccess {
                _snackbarMessage.value = "Đã đăng Tin 24h!"
                onDone()
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Không thể đăng Tin 24h."
            }
        }
    }

    fun toggleStoryLike(story: TwinStory) {
        viewModelScope.launch {
            repository.toggleStoryLike(story)
        }
    }

    fun sendFriendRequest(targetUid: String) {
        viewModelScope.launch {
            communityRepo.sendFriendRequest(targetUid, activeActor(), repository)
                .onSuccess { _snackbarMessage.value = "Đã gửi lời mời kết bạn." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể gửi lời mời." }
        }
    }

    fun cancelFriendRequest(targetUid: String) {
        viewModelScope.launch {
            communityRepo.cancelFriendRequest(targetUid)
        }
    }

    fun acceptFriendRequest(requesterUid: String) {
        viewModelScope.launch {
            communityRepo.acceptFriendRequest(requesterUid, activeActor(), repository)
                .onSuccess { _snackbarMessage.value = "Hai bạn đã trở thành bạn bè trên Twin!" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể chấp nhận lời mời." }
        }
    }

    fun declineFriendRequest(requesterUid: String) {
        viewModelScope.launch {
            communityRepo.declineFriendRequest(requesterUid)
        }
    }

    fun unfriend(targetUid: String) {
        viewModelScope.launch {
            communityRepo.unfriend(targetUid)
                .onSuccess { _snackbarMessage.value = "Đã hủy kết bạn." }
        }
    }

    fun createGroup(name: String, description: String, onDone: () -> Unit) {
        viewModelScope.launch {
            communityRepo.createGroup(name, description)
                .onSuccess {
                    _snackbarMessage.value = "Đã tạo nhóm mới!"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể tạo nhóm." }
        }
    }

    fun toggleJoinGroup(group: TwinGroup) {
        viewModelScope.launch {
            communityRepo.toggleJoinGroup(group)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Lỗi cập nhật nhóm." }
        }
    }

    fun createPage(name: String, category: String, description: String, onDone: () -> Unit) {
        viewModelScope.launch {
            communityRepo.createPage(name, category, description)
                .onSuccess {
                    _snackbarMessage.value = "Đã tạo Trang mới!"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể tạo Trang." }
        }
    }

    fun toggleFollowPage(page: TwinPage) {
        viewModelScope.launch {
            communityRepo.toggleFollowPage(page)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Lỗi theo dõi Trang." }
        }
    }

    fun sendChatMessage(
        otherUid: String,
        text: String,
        imageUri: Uri? = null,
        fileUri: Uri? = null,
        onSent: () -> Unit = {}
    ) {
        viewModelScope.launch {
            var uploadedImgUrl = ""
            var uploadedFileUrl = ""
            var uploadedFileName = ""
            if (imageUri != null) {
                _isUploadingMedia.value = true
                val uploadRes = uploader.uploadImage(imageUri)
                _isUploadingMedia.value = false
                if (uploadRes.isFailure) {
                    _snackbarMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "Gửi ảnh thất bại."
                    return@launch
                }
                uploadedImgUrl = uploadRes.getOrDefault("")
            }
            if (fileUri != null) {
                _isUploadingMedia.value = true
                val uploadRes = uploader.uploadAnyFile(fileUri)
                _isUploadingMedia.value = false
                if (uploadRes.isFailure) {
                    _snackbarMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "Gửi tệp thất bại."
                    return@launch
                }
                val asset = uploadRes.getOrNull()
                if (asset != null) {
                    uploadedFileUrl = asset.url
                    uploadedFileName = asset.fileName
                }
            }
            communityRepo.sendChatMessage(
                otherUid = otherUid,
                text = text,
                imageUrl = uploadedImgUrl,
                fileUrl = uploadedFileUrl,
                fileName = uploadedFileName,
                sender = activeActor(),
                mainRepo = repository
            ).onSuccess { onSent() }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể gửi tin nhắn." }
        }
    }

    fun deleteChatMessage(otherUid: String, messageId: String) {
        viewModelScope.launch {
            communityRepo.deleteChatMessage(otherUid, messageId)
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun markAllNotificationsRead() {
        val currentNotifs = (notificationsState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentNotifs)
                .onSuccess { _snackbarMessage.value = "Đã đánh dấu tất cả thông báo là đã đọc." }
        }
    }

    fun sendPasswordResetForCurrentUser() {
        val email = Firebase.auth.currentUser?.email
        if (email.isNullOrBlank()) {
            _snackbarMessage.value = "Tài khoản hiện tại không có địa chỉ email."
            return
        }
        viewModelScope.launch {
            try {
                Firebase.auth.sendPasswordResetEmail(email).await()
                _snackbarMessage.value = "Đã gửi liên kết đặt/đổi mật khẩu tới $email."
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Không thể gửi email đặt lại mật khẩu."
            }
        }
    }

    fun updateProfile(
        name: String,
        bio: String,
        location: String,
        work: String,
        website: String,
        newAvatarUri: Uri?,
        newCoverUri: Uri?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val current = activeActor()
            var avatarUrl = current.avatar
            var coverUrl = current.cover
            _isUploadingMedia.value = true
            if (newAvatarUri != null) {
                val res = uploader.uploadImage(newAvatarUri)
                if (res.isSuccess) avatarUrl = res.getOrDefault(avatarUrl)
                else {
                    _isUploadingMedia.value = false
                    _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Tải ảnh đại diện thất bại."
                    return@launch
                }
            }
            if (newCoverUri != null) {
                val res = uploader.uploadImage(newCoverUri)
                if (res.isSuccess) coverUrl = res.getOrDefault(coverUrl)
                else {
                    _isUploadingMedia.value = false
                    _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Tải ảnh bìa thất bại."
                    return@launch
                }
            }
            _isUploadingMedia.value = false
            repository.updateProfile(name, bio, location, work, website, avatarUrl, coverUrl)
                .onSuccess {
                    _snackbarMessage.value = "Đã cập nhật hồ sơ Twin!"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Không thể cập nhật hồ sơ." }
        }
    }

    suspend fun markOfflineBeforeSignOut() {
        repository.markOfflineBeforeSignOut()
    }

    private fun <T> Flow<T>.toUiState(defaultError: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, defaultError, root)
                emit(UiState.Error(root.message ?: defaultError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        private const val TAG = "TwinMainVM"
        private const val STOP_TIMEOUT_MS = 5000L

        fun factory(
            currentUserId: String,
            repository: TwinRepository,
            communityRepo: TwinCommunityRepository,
            uploader: CloudinaryUploader
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TwinMainViewModel(currentUserId, repository, communityRepo, uploader) as T
        }
    }
}
