package com.github.lamba92.tln.summarization

import io.ktor.util.*
import org.apache.commons.math3.util.MathArrays

fun DoubleArray.euclideanNorm() =
    MathArrays.distance(this, DoubleArray(this.size) { 0.0 })

fun DoubleArray.cosineSimilarityWith(other: DoubleArray) =
    cosineSimilarity(this, other)

fun cosineSimilarity(array1: DoubleArray, array2: DoubleArray) =
    dotProduct(array1, array2) / (array1.euclideanNorm() * array2.euclideanNorm())

fun DoubleArray.dotProductWith(other: DoubleArray) =
    dotProduct(this, other)

fun dotProduct(array1: DoubleArray, array2: DoubleArray) =
    array1.zip(array2).foldRight(0.0) { (n1, n2), acc ->
        acc + n1 * n2
    }

@ExperimentalStdlibApi
@KtorExperimentalAPI
suspend fun MiniNasari.senseSimilarity(word1: String, word2: String): SenseIdentificationResult? {

    val w1NasariMeanings = BabelNetApi.lookupBabelSynsetsByLemma(word1)
        .mapNotNull { id -> this[id]?.let { id to it } }
        .toMap()

    val w2NasariMeanings = BabelNetApi.lookupBabelSynsetsByLemma(word2)
        .mapNotNull { id -> this[id]?.let { id to it } }
        .toMap()

    return buildMap<Pair<String, String>, Double> {
        w1NasariMeanings.forEach { (idW1, w1Array) ->
            w2NasariMeanings.forEach { (idW2, w2Array) ->
                put(idW1 to idW2, cosineSimilarity(w1Array, w2Array))
            }
        }
    }
        .maxByOrNull { it.value }
        ?.let {
            SenseIdentificationResult(
                word1,
                it.key.first,
                word2,
                it.key.second,
                it.value
            )
        }

}

data class SenseIdentificationResult(
    val word1: String,
    val word1BabelSynsetId: String,
    val word2: String,
    val word2BabelSynsetId: String,
    val cosineSimilarity: Double
)

typealias MiniNasari = Map<String, DoubleArray>

fun Iterable<Double>.mean() = with(iterator()) {
    if (!hasNext())
        error("Iterable is empty")
    var sum = 0.0
    var size = 0
    while (hasNext()) {
        size++
        sum += next()
    }
    sum / size
}
