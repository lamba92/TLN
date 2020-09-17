package com.github.lamba92.tln.summarization.nasari

import com.github.lamba92.tln.summarization.BabelNetApi
import com.github.lamba92.tln.summarization.NasariComparisonItem
import com.github.lamba92.tln.summarization.NasariUnified
import com.github.lamba92.tln.summarization.NasariUnifiedElement
import io.ktor.util.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.math.sqrt

@FlowPreview
suspend inline fun <T, R, K> Flow<T>.combineWith(other: Iterable<R>, crossinline function: suspend (T, R) -> K) =
    flatMapConcat { t: T -> other.map { r: R -> function(t, r) }.asFlow() }

@KtorExperimentalAPI
suspend fun NasariUnified.getVectorsByLemma(lemma: String, lang: String) =
    BabelNetApi.lookupBabelSynsetsByLemma(lemma, lang).mapNotNull { get(it) }

@KtorExperimentalAPI
@FlowPreview
suspend fun NasariUnified.weightedOverlap(
    sentence: Sequence<String>,
    contexts: List<NasariUnifiedElement>,
    lang: String
) =
    weightedOverlap(sentence.toList(), contexts, lang)

@FlowPreview
@KtorExperimentalAPI
suspend fun NasariUnified.weightedOverlap(sentence: List<String>, contexts: List<NasariUnifiedElement>, lang: String) =
    sentence.asFlow()
        .map {
            getVectorsByLemma(it, lang)
                .asFlow()
                .combineWith(contexts) { vector1, vector2 -> sqrt(weightedOverlap(vector1.data, vector2.data)) }
                .toList()
                .maxByOrNull { it } ?: 0.0
        }
        .toList()
        .sumByDouble { it }

fun rank(elem: String, vector2: List<NasariComparisonItem>) =
    vector2.indexOfFirst { it.name == elem } + 1

fun weightedOverlap(vector1: List<NasariComparisonItem>, vector2: List<NasariComparisonItem>): Double {

    val m1 = vector1.associate { it.name to it.score }
    val m2 = vector2.associate { it.name to it.score }

    val keyIntersection = m1.keys.filter { it in m2.keys }.toSet()

    return if (keyIntersection.isNotEmpty()) {
        val num = keyIntersection.sumByDouble { word -> 1.0 / (rank(word, vector1) + rank(word, vector2)) }
        val den = (1 until keyIntersection.size + 1).sumByDouble { index -> 1.0 / (2 * index) }
        num / den
    } else
        0.0

}
