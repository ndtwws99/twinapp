package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UploadedAsset(
    val url: String,
    val fileName: String,
    val mimeType: String
)

/**
 * Unsigned Cloudinary upload client matching Twin web's assets/js/config.js
 * (cloudName = "oaagraxi", uploadPreset = "ndtwws99"). Supports images and general files.
 */
class CloudinaryUploader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(uri: Uri): Result<String> =
        uploadMedia(uri, resourceType = "image").map { it.url }

    suspend fun uploadAnyFile(uri: Uri): Result<UploadedAsset> {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val resourceType = when {
            mime.startsWith("image/") -> "image"
            mime.startsWith("video/") -> "video"
            else -> "auto"
        }
        return uploadMedia(uri, resourceType = resourceType)
    }

    private suspend fun uploadMedia(uri: Uri, resourceType: String): Result<UploadedAsset> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                val fileName = queryDisplayName(uri) ?: "twin_upload_${System.currentTimeMillis()}"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalStateException("Không thể đọc tệp đã chọn."))

                if (bytes.size > MAX_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Tệp vượt quá giới hạn 15MB.")
                    )
                }

                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .addFormDataPart(
                        "file",
                        fileName,
                        bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/$resourceType/upload")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Tải tệp lên thất bại (HTTP ${response.code}).")
                        )
                    }
                    val json = JSONObject(raw)
                    val secureUrl = json.optString("secure_url")
                    if (secureUrl.isBlank()) {
                        Result.failure(IllegalStateException("Máy chủ không trả về liên kết tệp."))
                    } else {
                        Result.success(
                            UploadedAsset(
                                url = secureUrl,
                                fileName = fileName,
                                mimeType = mimeType
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val CLOUD_NAME = "oaagraxi"
        private const val UPLOAD_PRESET = "ndtwws99"
        private const val MAX_BYTES = 15 * 1024 * 1024
    }
}
