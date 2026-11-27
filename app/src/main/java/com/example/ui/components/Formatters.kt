package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

fun formatCompactCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 10_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    else -> count.toString()
}

fun formatByteSize(bytes: Long): String = when {
    bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024L -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
    else -> "$bytes B"
}

fun formatRelativeTimeVi(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    val parsedMs = parseIso8601Millis(isoTimestamp) ?: return isoTimestamp.take(10)
    val diffSec = abs((System.currentTimeMillis() - parsedMs) / 1000L)
    return when {
        diffSec < 60 -> "vừa xong"
        diffSec < 3600 -> "${diffSec / 60} phút trước"
        diffSec < 86_400 -> "${diffSec / 3600} giờ trước"
        diffSec < 2_592_000 -> "${diffSec / 86_400} ngày trước"
        diffSec < 31_536_000 -> "${diffSec / 2_592_000} tháng trước"
        else -> "${diffSec / 31_536_000} năm trước"
    }
}

private fun parseIso8601Millis(iso: String): Long? {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(iso)
            if (date != null) return date.time
        } catch (_: Exception) {
            // Try next format
        }
    }
    return null
}
