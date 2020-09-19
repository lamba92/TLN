package com.github.lamba92.tln.summarization

import org.apache.commons.codec.digest.DigestUtils
import java.io.File

data class NasariComparisonItem(val lemma: String, val score: Double)
data class NasariUnifiedArray(val babelNetId: String, val lemma: String, val data: List<NasariComparisonItem>)

typealias NasariUnified = Map<String, NasariUnifiedArray>

private fun Collection<String>.mergeIf(function: (String, String) -> Boolean): Collection<String> {
    if (size < 2)
        return this
    val iter = iterator()
    var left = iter.next()
    val result = mutableListOf<String>()
    while (iter.hasNext()) {
        val right = iter.next()
        left = if (function(left, right))
            "$left;$right"
        else {
            result.add(left)
            right
        }
    }
    return result
}

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

fun File.md5() =
    DigestUtils.md5Hex(inputStream())!!
