package com.github.lamba92.tln.conceptualsimilarity

import com.github.lamba92.tln.evaluation.*
import it.lamba.utils.getResource
import net.sf.extjwnl.dictionary.Dictionary

data class ResourceLine(val word1: String, val word2: String, val score: Double)

@ExperimentalStdlibApi
fun main() {

    val wordSim = extractWordSimResource()

    val dictionary = Dictionary.getDefaultResourceInstance()!!
    val depth = dictionary.maxDepth()

    val targetValues = wordSim.map { it.score }

    val wpScores = wordSim.map { dictionary.wpSimilarity(it.word1, it.word2) }
    val lcScores = wordSim.map { dictionary.lcSimilarity(it.word1, it.word2, maxDepth = depth) }
    val spScores = wordSim.map { dictionary.spSimilarity(it.word1, it.word2, maxDepth = depth) }

    println("Pearson Correlation: ")
    println("WP: " + pearsonCorrelationCoefficient(wpScores, targetValues))
    println("LC: " + pearsonCorrelationCoefficient(lcScores, targetValues))
    println("SP: " + pearsonCorrelationCoefficient(spScores, targetValues))

    println("Spearman Correlation: ")
    println("WP: " + spearmanRankCorrelationCoefficient(wpScores, targetValues))
    println("LC: " + spearmanRankCorrelationCoefficient(lcScores, targetValues))
    println("SP: " + spearmanRankCorrelationCoefficient(spScores, targetValues))
}

fun extractWordSimResource() =
    getResource("WordSim353.csv")
        .readLines()
        .asSequence()
        .drop(1)
        .filter { it.isNotEmpty() }
        .map {
            it.split(",").let { (w1, w2, s) ->
                ResourceLine(w1.filter { !it.isWhitespace() }, w2.filter { !it.isWhitespace() }, s.toDouble() / 10)
            }
        }
        .toList()
