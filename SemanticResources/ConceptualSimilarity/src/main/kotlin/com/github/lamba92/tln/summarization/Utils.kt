package com.github.lamba92.tln.summarization

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
                ResourceLine(w1.filter { !it.isWhitespace() }, w2.filter { !it.isWhitespace() }, s.toDouble() / 10)
            }
        }
}
