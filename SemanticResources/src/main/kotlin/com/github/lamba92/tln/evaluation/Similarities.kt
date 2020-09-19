package com.github.lamba92.tln.evaluation

import net.sf.extjwnl.dictionary.Dictionary
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

@ExperimentalStdlibApi
fun Dictionary.wpSimilarity(word1: String, word2: String): Double {
    var max = 0.0
    val w2Synsets = synsets(word2)
    synsets(word1).forEach { s1 ->
        w2Synsets.forEach { s2 ->
            s1.lowestCommonHypernyms(s2).forEach { lcs ->
                val similarity = 2.0 * lcs.maxDepth().toDouble() / (s1.maxDepth() + s2.maxDepth())
                max = max(max, similarity)
            }
        }
    }
    return max
}

@ExperimentalStdlibApi
fun Dictionary.lcSimilarity(
    word1: String,
    word2: String,
    normalized: Boolean = true,
    maxDepth: Int = 20
): Double {
    var max = 0.0
    val w2Synsets = synsets(word2)
    synsets(word1).forEach { s1 ->
        w2Synsets.forEach { s2 ->
            val dist = s1.shortestPathDistance(s2)
            val sim = when {
                dist == null -> 0.0
                dist > 0 -> -log10(dist.toDouble() / (2 * maxDepth))
                else -> -log10(dist.toDouble() / (2 * maxDepth))
            }
            max = max(max, sim)
        }
    }
    return if (normalized) max / log10((2 * maxDepth + 1).toDouble()) else max
}

fun Dictionary.spSimilarity(
    word1: String,
    word2: String,
    normalized: Boolean = true,
    maxDepth: Int = 20
): Double {
    var minDistance = maxDepth
    val w2Synsets = synsets(word2)
    synsets(word1).forEach { s1 ->
        w2Synsets.forEach { s2 ->
            val distS1ToS2 = s1.shortestPathDistance(s2) ?: 2 * maxDepth
            minDistance = min(minDistance, distS1ToS2)
        }
    }
    val similarity = 2 * maxDepth - minDistance
    return if (normalized) similarity.toDouble() / (2.0 * maxDepth) else similarity.toDouble()
}
