package com.github.lamba92.tln.summarization.nasari

import com.github.lamba92.tln.summarization.NasariComparisonItem
import com.github.lamba92.tln.summarization.NasariUnifiedArray
import io.ktor.util.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.math.sqrt

@FlowPreview
suspend inline fun <T, R, K> Flow<T>.combineWith(other: Iterable<R>, crossinline function: suspend (T, R) -> K) =
    flatMapConcat { t: T -> other.map { r: R -> function(t, r) }.asFlow() }

@FlowPreview
@KtorExperimentalAPI
suspend fun NasariApi.weightedOverlap(sentence: List<String>, contexts: List<NasariUnifiedArray>, lang: String) =
    sentence.asFlow()
        .map {
            lookupArraysByLemma(it, lang)
                .asFlow()
                .combineWith(contexts) { vector1, vector2 -> senseSimilarity(vector1, vector2) }
                .toList()
                .maxByOrNull { it } ?: 0.0
        }
        .toList()
        .sumByDouble { it }

fun senseSimilarity(vector1: NasariUnifiedArray, vector2: NasariUnifiedArray) =
    sqrt(weightedOverlap(vector1.data, vector2.data))

fun weightedOverlap(vector1: List<NasariComparisonItem>, vector2: List<NasariComparisonItem>): Double {

    val m1 = vector1.associate { it.lemma to it.score }
    val m2 = vector2.associate { it.lemma to it.score }

    val keyIntersection = m1.keys.filter { it in m2.keys }.toSet()

    return if (keyIntersection.isNotEmpty()) {
        val num = keyIntersection.sumByDouble { word -> 1.0 / (rank(word, vector1) + rank(word, vector2)) }
        val den = (1 until keyIntersection.size + 1).sumByDouble { index -> 1.0 / (2 * index) }
        num / den
    } else
        0.0

}

fun rank(elem: String, vector2: List<NasariComparisonItem>) =
    vector2.indexOfFirst { it.lemma == elem } + 1
