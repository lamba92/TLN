package com.github.lamba92.tln

import io.ktor.util.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.sf.extjwnl.data.POS

data class BabelNetSynsetId(val id: Long, val pos: POS, val name: String? = null) {
    override fun toString() =
        "bn:$id${pos.key}"
}

data class ManualAnnotation(val word1: String, val word2: String, val score: Float)

@KtorExperimentalAPI
@ExperimentalStdlibApi
suspend fun main() {

    val basti = Resources.getAnnotatedPairs("annotations/basti.txt")
    val pregno = Resources.getAnnotatedPairs("annotations/pregno.txt")

    val pearson = pearsonCorrelationCoefficient(
        basti.map { it.score.toDouble() },
        pregno.map { it.score.toDouble() }
    )

    val spearman = spearmanRankCorrelationCoefficient(
        basti.map { it.score.toDouble() },
        pregno.map { it.score.toDouble() }
    )

    println("Pearson correlation value is: $pearson")
    println("Spearman correlation value is: $spearman")

    val nasari = Resources.MINI_NASARI

    val nasariSimilarityScores = basti
        .asFlow()
        .map { nasari.senseSimilarity(it.word1, it.word2) }
        .toList()

    println("Best senses found:")
    nasariSimilarityScores.filterNotNull().forEach { (w1, s1, w2, s2, cs) ->
        println(" - $w1 -> $s1 | $w2 -> $s2 | cosine similarity: $cs")
    }

    val nasariSimilarityScoresMean =
        nasariSimilarityScores.mapNotNull { it?.cosineSimilarity }
            .mean()

    val bastiNasariPearsonScore = pearsonCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        basti.map { it.score.toDouble() }
    )

    val bastiNasariSpearmanScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        basti.map { it.score.toDouble() }
    )

    val pregnoNasariPearsonScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        pregno.map { it.score.toDouble() }
    )

    val pregnoNasariSpearmanScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        pregno.map { it.score.toDouble() }
    )

    println("Scores using Nasari:")
    println(" - Basti | Pearson: $bastiNasariPearsonScore")
    println(" - Basti | Spearman: $bastiNasariSpearmanScore")
    println(" - Pregno | Pearson: $pregnoNasariPearsonScore")
    println(" - Pregno | Spearman: $pregnoNasariSpearmanScore")


}

