package com.github.lamba92.tln

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
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

typealias MiniNasari = Map<String, DoubleArray>

@ExperimentalStdlibApi
@KtorExperimentalAPI
suspend fun senseSimilarity(word1: String, word2: String, nasari: MiniNasari): Double {

    val w1NasariMeanings = lookupBabelSynsetsByLemma(word1)
        .mapNotNull { nasari[it] }
    val w2NasariMeanings = lookupBabelSynsetsByLemma(word2)
        .mapNotNull { nasari[it] }
    return buildList {
        w1NasariMeanings.forEach { w1Array ->
            w2NasariMeanings.forEach { w2Array ->
                add(cosineSimilarity(w1Array, w2Array))
            }
        }
    }.maxOf { it }

}

@KtorExperimentalAPI
val HTTP_CLIENT by lazy {
    HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
}

val BABEL_NET_API_KEY: String by System.getenv()

@Serializable
data class BabelNetIdQuery(val id: String, val pos: String, val source: String)

@KtorExperimentalAPI
suspend fun lookupBabelSynsetsByLemma(lemma: String) =
    HTTP_CLIENT.get<List<BabelNetIdQuery>>("https://babelnet.io/v5/getSynsetIds") {
        parameter("lemma", lemma)
        parameter("searchLang", "IT")
        parameter("key", BABEL_NET_API_KEY)
    }.map { it.id }
