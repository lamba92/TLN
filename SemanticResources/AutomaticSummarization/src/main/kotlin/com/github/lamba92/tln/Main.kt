package com.github.lamba92.tln

import com.github.lamba92.tln.nasari.getVectorsByLemma
import com.github.lamba92.tln.nasari.weightedOverlap
import io.ktor.util.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import java.io.File

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

@FlowPreview
@KtorExperimentalAPI
@ExperimentalStdlibApi
suspend fun main() {

    val nasari = Resources.getNasariUnified()
    val output = File("summarization.txt")
        .apply {
            if (exists())
                delete()
            createNewFile()
        }
    Resources.Corpus.ALL.forEach { document ->

        val titleContext = document.title
            .tokenize()
            .asFlow()
            .map { nasari.getVectorsByLemma(it, "EN") }
            .toList()

        val (index, score) = document.paragraphs
            .asFlow()
            .map { paragraph ->
                val words = paragraph
                    .tokenize()
                    .filter { it !in Resources.STOPWORDS }
                    .toList()
                titleContext.sumByDouble { nasari.weightedOverlap(words, it, "EN") }
            }
            .withIndex()
            .toList()
            .maxByOrNull { it.value }!!

        output.appendText("\nDocument titled '${document.title}' best paragraph, with score $score is:")
        output.appendText("\n - ${document.paragraphs[index]}")
        output.appendText("\n____________________________________________________________________________")

    }
}
