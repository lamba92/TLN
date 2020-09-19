package com.github.lamba92.tln.summarization

import com.github.lamba92.tln.nasari.NasariApi
import com.github.lamba92.tln.summarization.nasari.weightedOverlap
import io.ktor.util.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import java.io.File

@FlowPreview
@KtorExperimentalAPI
@ExperimentalStdlibApi
suspend fun main() {

    val output = File("summarization.txt").apply {
        if (exists())
            delete()
        createNewFile()
    }
    Resources.Corpus.ALL.forEach { document ->

        val titleContext = document.title
            .tokenize()
            .asFlow()
            .map { NasariApi.lookupArraysByLemma(it, "EN") }
            .toList()

        val (index, score) = document.paragraphs.asFlow()
            .map { it.tokenize().filter { it !in Resources.STOPWORDS } }
            .map { paragraphWords -> titleContext.sumByDouble { NasariApi.weightedOverlap(paragraphWords, it, "EN") } }
            .withIndex()
            .toList()
            .maxByOrNull { it.value }!!

        output.appendText("\nDocument titled '${document.title}' best paragraph, with score $score is:")
        output.appendText("\n - ${document.paragraphs[index]}")
        output.appendText("\n____________________________________________________________________________")

    }
}
