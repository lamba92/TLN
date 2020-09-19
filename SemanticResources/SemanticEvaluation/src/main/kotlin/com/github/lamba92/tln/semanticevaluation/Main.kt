package com.github.lamba92.tln.semanticevaluation

import com.github.lamba92.tln.evaluation.pearsonCorrelationCoefficient
import com.github.lamba92.tln.evaluation.spearmanRankCorrelationCoefficient
import io.ktor.util.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

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

    val nasari = Resources.MINI_NASARI

    val nasariSimilarityScores = basti
        .asFlow()
        .map { nasari.senseSimilarity(it.word1, it.word2) }
        .toList()

    println("Basti - Pregno Pearson correlation value is: " + "%.4f".format(pearson))
    println("Basti - Pregno Spearman correlation value is: " + "%.4f".format(spearman))

    println("Best senses found:")
    nasariSimilarityScores.filterNotNull().forEach { (w1, s1, w2, s2, cs) ->
        println(" - $w1 -> $s1 | $w2 -> $s2 | cosine similarity: " + "%.2f".format(cs))
    }

    val nasariSimilarityScoresMean =
        nasariSimilarityScores.mapNotNull { it?.cosineSimilarity }
            .mean()

    val bastiNasariPearsonScore = pearsonCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        basti.map { it.score.toDouble() / 4 }
    )

    val bastiNasariSpearmanScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        basti.map { it.score.toDouble() / 4 }
    )

    val pregnoNasariPearsonScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        pregno.map { it.score.toDouble() / 4 }
    )

    val pregnoNasariSpearmanScore = spearmanRankCorrelationCoefficient(
        nasariSimilarityScores.map { it?.cosineSimilarity ?: nasariSimilarityScoresMean },
        pregno.map { it.score.toDouble() / 4 }
    )

    println("Scores using Nasari:")
    println(" - Basti | Pearson: " + "%.4f".format(bastiNasariPearsonScore))
    println(" - Basti | Spearman: " + "%.4f".format(bastiNasariSpearmanScore))
    println(" - Pregno | Pearson: " + "%.4f".format(pregnoNasariPearsonScore))
    println(" - Pregno | Spearman: " + "%.4f".format(pregnoNasariSpearmanScore))


}

