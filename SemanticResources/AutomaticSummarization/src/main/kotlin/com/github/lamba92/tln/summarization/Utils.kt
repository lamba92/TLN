package com.github.lamba92.tln.summarization

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.codec.digest.DigestUtils
import java.io.File

data class NasariComparisonItem(val lemma: String, val score: Double)
data class NasariUnifiedArray(val babelNetId: String, val data: List<NasariComparisonItem>)

fun parseDocument(f: File): Document {
    val lines = f.readLines()
        .filter { it.isNotBlank() }
    return Document(lines.first(), lines[1], lines.drop(2))
}

data class Document(val link: String, val title: String, val paragraphs: List<String>)

fun String.tokenize() =
    split(" ")
        .asSequence()
        .flatMap { it.split(".") }
        .flatMap { it.split(",") }
        .flatMap { it.split(";") }
        .map { it.filter { it.isLetterOrDigit() } }
        .map { it.toLowerCase() }
        .filter { it.isNotBlank() }
        .filter { it.isNotEmpty() }
        .toList()

fun File.md5Hex() =
    DigestUtils.md5Hex(inputStream())!!

suspend inline fun <T, R> T.letWithContext(
    dispatcher: CoroutineDispatcher, crossinline block: (T) -> R
) = let {
    withContext(dispatcher) {
        block(it)
    }
}
