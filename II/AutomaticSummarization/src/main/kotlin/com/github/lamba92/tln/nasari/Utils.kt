package com.github.lamba92.tln.nasari

import com.github.lamba92.tln.BabelNetApi
import com.github.lamba92.tln.NasariComparisonItem
import com.github.lamba92.tln.NasariUnified
import com.github.lamba92.tln.NasariUnifiedElement
import io.ktor.util.*
import kotlinx.coroutines.FlowPreview

inline fun <T, R, K> Iterable<T>.combineWith(other: Iterable<R>, function: (T, R) -> K) =
    flatMap { t: T -> other.map { r: R -> function(t, r) } }

@KtorExperimentalAPI
suspend fun NasariUnified.getVectorsByLemma(lemma: String, lang: String) =
    BabelNetApi.lookupBabelSynsetsByLemma(lemma, lang)
        .mapNotNull { get(it) }

@FlowPreview
@KtorExperimentalAPI
suspend fun NasariUnified.weightedOverlap(sentence: List<String>, contexts: List<NasariUnifiedElement>, lang: String) =
    sentence
//        .asFlow()
        .map { getVectorsByLemma(it, lang) }
        .map {
            it.combineWith(contexts) { ctx1, ctx2 -> ctx1 to ctx2 }
//                .asFlow()
                .map { (ctx1, ctx2) ->
                    weightedOverlap(ctx1.data, ctx2.data)
                }
//                .toList()
                .maxByOrNull {
                    it
                } ?: 0.0
        }
//        .toList()
        .sumByDouble { it }

fun rank(elem: String, vector2: List<NasariComparisonItem>) =
    vector2.indexOfFirst { it.name == elem } + 1

fun weightedOverlap(context1: List<NasariComparisonItem>, context2: List<NasariComparisonItem>): Double {

    val m1 = context1.associate { it.name to it.score }
    val m2 = context2.associate { it.name to it.score }

    val keyIntersection = m1.keys.filter { it in m2.keys }.toSet()

    return if (keyIntersection.isNotEmpty()) {
        val num = keyIntersection.sumByDouble { word -> 1.0 / (rank(word, context1) + rank(word, context2)) }
        val den = (1 until keyIntersection.size + 1).sumByDouble { index -> 1.0 / (2 * index) }
        num / den
    } else
        0.0

}
