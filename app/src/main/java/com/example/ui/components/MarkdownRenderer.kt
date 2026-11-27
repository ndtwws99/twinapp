package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MonoCodeStyle

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val language: String, val code: String) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class Bullet(val marker: String, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data object Divider : MdBlock()
}

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    maxBlocks: Int = 160
) {
    val blocks = remember(markdown, maxBlocks) {
        parseMarkdownBlocks(markdown).take(maxBlocks)
    }

    val linkColor = MaterialTheme.colorScheme.secondary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val codeTextColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        }
                        Text(
                            text = formatInlineMarkdown(block.text, linkColor, codeBg, codeTextColor),
                            style = style,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (block.level <= 2) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )
                        }
                    }
                }

                is MdBlock.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        if (block.language.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = block.language.lowercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = block.code,
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }

                is MdBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = formatInlineMarkdown(block.text, linkColor, codeBg, codeTextColor),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MdBlock.Bullet -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = block.marker,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatInlineMarkdown(block.text, linkColor, codeBg, codeTextColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MdBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text, linkColor, codeBg, codeTextColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                MdBlock.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(raw: String): List<MdBlock> {
    val lines = raw.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    var inCodeBlock = false
    var codeLang = ""
    val codeLines = mutableListOf<String>()
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isNotEmpty()) {
            val cleaned = cleanHtmlTags(paragraphLines.joinToString(" ").trim())
            if (cleaned.isNotBlank()) {
                blocks.add(MdBlock.Paragraph(cleaned))
            }
            paragraphLines.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            flushParagraph()
            if (!inCodeBlock) {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
                codeLines.clear()
            } else {
                inCodeBlock = false
                blocks.add(MdBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
                codeLines.clear()
                codeLang = ""
            }
            continue
        }

        if (inCodeBlock) {
            codeLines.add(line)
            continue
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            continue
        }

        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            flushParagraph()
            blocks.add(MdBlock.Divider)
            continue
        }

        // Skip raw HTML comment or image-only badge rows
        if (trimmed.startsWith("<!--") || (trimmed.startsWith("<") && trimmed.endsWith(">") && cleanHtmlTags(trimmed).isBlank())) {
            continue
        }

        val headingMatch = Regex("^(#{1,4})\\s+(.+)$").find(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val text = cleanHtmlTags(headingMatch.groupValues[2].trim())
            if (text.isNotBlank()) {
                blocks.add(MdBlock.Heading(level, text))
            }
            continue
        }

        if (trimmed.startsWith(">")) {
            flushParagraph()
            val text = cleanHtmlTags(trimmed.removePrefix(">").trim())
            if (text.isNotBlank()) {
                blocks.add(MdBlock.Quote(text))
            }
            continue
        }

        val bulletMatch = Regex("^([-*+]|\\d+\\.)\\s+(.+)$").find(trimmed)
        if (bulletMatch != null) {
            flushParagraph()
            val rawMarker = bulletMatch.groupValues[1]
            val marker = if (rawMarker.endsWith(".")) rawMarker else "•"
            val text = cleanHtmlTags(bulletMatch.groupValues[2].trim())
            if (text.isNotBlank()) {
                blocks.add(MdBlock.Bullet(marker, text))
            }
            continue
        }

        paragraphLines.add(trimmed)
    }

    if (inCodeBlock && codeLines.isNotEmpty()) {
        blocks.add(MdBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
    }
    flushParagraph()
    return blocks
}

private fun cleanHtmlTags(input: String): String {
    // Strip markdown images ![alt](url) -> alt if helpful, else empty
    val withoutMdImages = input.replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "")
    // Strip HTML tags while keeping readable text
    return withoutMdImages
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

private fun formatInlineMarkdown(
    text: String,
    linkColor: Color,
    codeBg: Color,
    codeColor: Color
): AnnotatedString = buildAnnotatedString {
    val tokenRegex = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`|\\[[^\\]]+\\]\\([^)]+\\)|\\*[^*]+\\*)")
    var cursor = 0
    for (match in tokenRegex.findAll(text)) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.removeSurrounding("**"))
                }
            }
            token.startsWith("`") && token.endsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = JetBrainsMono,
                        background = codeBg,
                        color = codeColor
                    )
                ) {
                    append(" ${token.removeSurrounding("`")} ")
                }
            }
            token.startsWith("[") && token.contains("](") -> {
                val label = token.substringAfter("[").substringBefore("](")
                withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                    append(label)
                }
            }
            token.startsWith("*") && token.endsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.removeSurrounding("*"))
                }
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}
