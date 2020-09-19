package com.github.lamba92.tln.evaluation

import org.apache.commons.math3.stat.correlation.Covariance
import org.apache.commons.math3.stat.ranking.NaturalRanking
import org.nield.kotlinstatistics.standardDeviation

fun pearsonCorrelationCoefficient(
    targetValues: List<Double>,
    obtainedValues: List<Double>
) =
    targetValues.covarianceWith(obtainedValues) /
            (targetValues.standardDeviation() *
                    obtainedValues.standardDeviation())

fun spearmanRankCorrelationCoefficient(
    targetValues: List<Double>,
    obtainedValues: List<Double>
): Double {
    val t = targetValues.naturalRanking()
    val o = obtainedValues.naturalRanking()
    return t.covarianceWith(o) / (t.standardDeviation() * o.standardDeviation())
}

fun List<Double>.naturalRanking() =
    NaturalRanking().rank(toDoubleArray())!!

fun List<Double>.covarianceWith(other: List<Double>) =
    Covariance().covariance(this.toDoubleArray(), other.toDoubleArray())

fun DoubleArray.covarianceWith(other: DoubleArray) =
    Covariance().covariance(this, other)
