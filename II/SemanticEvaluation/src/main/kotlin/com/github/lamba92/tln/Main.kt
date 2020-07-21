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

    val basti = Resources.getAnnotatedPairs("annotations/basti.txt", items = 2)
    val pregno = Resources.getAnnotatedPairs("annotations/pregno.txt", items = 2)

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

    val nasariSimilarityScores = basti.asFlow()
        .map { senseSimilarity(it.word1, it.word2, nasari) }.toList()

    val bastiNasariPearsonScore = pearsonCorrelationCoefficient(
        nasariSimilarityScores.map { it.cosineSimilarity },
        basti.map { it.score.toDouble() }
    )

    val bastiNasariSpearmanScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it.cosineSimilarity },
        basti.map { it.score.toDouble() }
    )

    val pregnoNasariPearsonScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it.cosineSimilarity },
        pregno.map { it.score.toDouble() }
    )

    val pregnoNasariSpearmanScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it.cosineSimilarity },
        pregno.map { it.score.toDouble() }
    )

    println("Scores using Nasari:")
    println(" - Basti | Pearson: $bastiNasariPearsonScore")
    println(" - Basti | Spearman: $bastiNasariSpearmanScore")
    println(" - Pregno | Pearson: $pregnoNasariPearsonScore")
    println(" - Pregno | Spearman: $pregnoNasariSpearmanScore")

}
