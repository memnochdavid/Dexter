package com.david.pokedex_api.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Convierte HTML basico a AnnotatedString de Compose.
 * Soporta: <b>, <strong>, <i>, <em>, <u>, <br>, <p>, <ul>/<li>, <a>.
 * Tags desconocidos se ignoran y se renderiza su contenido interno.
 */
fun htmlToAnnotatedString(
    html: String,
    linkColor: Color = Color.Unspecified
): AnnotatedString {
    val doc = Jsoup.parseBodyFragment(html)
    return buildAnnotatedString {
        processNode(doc.body(), this, linkColor)
        // Limpiar saltos de linea redundantes al final
        val text = toAnnotatedString()
        if (text.text.endsWith("\n")) {
            // Se reconstruye sin trailing newlines en el caller si es necesario
        }
    }.trimTrailingNewlines()
}

private fun processNode(
    node: Node,
    builder: AnnotatedString.Builder,
    linkColor: Color
) {
    for (child in node.childNodes()) {
        when (child) {
            is TextNode -> {
                val text = child.wholeText
                    .replace("\r\n", " ")
                    .replace("\n", " ")
                    .replace("\r", " ")
                    // Colapsar espacios multiples
                    .replace(Regex("  +"), " ")
                if (text.isNotEmpty()) {
                    builder.append(text)
                }
            }
            is Element -> {
                when (child.tagName().lowercase()) {
                    "br" -> builder.append("\n")

                    "p" -> {
                        ensureNewline(builder)
                        processNode(child, builder, linkColor)
                        ensureNewline(builder)
                    }

                    "b", "strong" -> {
                        val start = builder.length
                        processNode(child, builder, linkColor)
                        builder.addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            start, builder.length
                        )
                    }

                    "i", "em" -> {
                        val start = builder.length
                        processNode(child, builder, linkColor)
                        builder.addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            start, builder.length
                        )
                    }

                    "u" -> {
                        val start = builder.length
                        processNode(child, builder, linkColor)
                        builder.addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start, builder.length
                        )
                    }

                    "a" -> {
                        val start = builder.length
                        processNode(child, builder, linkColor)
                        if (linkColor != Color.Unspecified) {
                            builder.addStyle(
                                SpanStyle(color = linkColor),
                                start, builder.length
                            )
                        }
                    }

                    "ul", "ol" -> {
                        ensureNewline(builder)
                        processNode(child, builder, linkColor)
                    }

                    "li" -> {
                        ensureNewline(builder)
                        builder.append("• ")
                        processNode(child, builder, linkColor)
                    }

                    // Tags desconocidos: renderizar contenido interno
                    else -> processNode(child, builder, linkColor)
                }
            }
        }
    }
}

/**
 * Asegura que el builder termine con un salto de linea (sin duplicar).
 */
private fun ensureNewline(builder: AnnotatedString.Builder) {
    val current = builder.toAnnotatedString().text
    if (current.isNotEmpty() && !current.endsWith("\n")) {
        builder.append("\n")
    }
}

/**
 * Elimina saltos de linea al inicio y final del AnnotatedString.
 */
private fun AnnotatedString.trimTrailingNewlines(): AnnotatedString {
    val text = this.text
    val start = text.indexOfFirst { it != '\n' && it != ' ' }
    val end = text.indexOfLast { it != '\n' && it != ' ' }
    if (start == -1 || end == -1) return AnnotatedString("")
    if (start == 0 && end == text.length - 1) return this
    return subSequence(start, end + 1)
}
