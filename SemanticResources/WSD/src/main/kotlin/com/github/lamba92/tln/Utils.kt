package com.github.lamba92.tln

fun extractWordSimResource(): List<ResourceLine> {
    return Thread.currentThread().contextClassLoader
        .getResourceAsStream("WordSim353.csv")!!
        .readBytes()
        .decodeToString()
        .split("\n")
        .drop(1)
        .filter { it.isNotEmpty() }
        .map {
            it.split(",").let { (w1, w2, s) ->
                ResourceLine(w1, w2, s.toDouble() / 10)
            }
        }
}
