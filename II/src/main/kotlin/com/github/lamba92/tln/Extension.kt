package com.github.lamba92.tln

import net.sf.extjwnl.data.POS
import net.sf.extjwnl.data.PointerType
import net.sf.extjwnl.data.Synset
import net.sf.extjwnl.dictionary.Dictionary

val Synset.border: List<Synset>
    get() = pointers.map { it.targetSynset }

val Synset.hypernyms
    get() = pointers.filter { it.type == PointerType.HYPERNYM }
        .map { it.targetSynset!! }

@ExperimentalStdlibApi
fun Synset.lowestCommonHypernyms(other: Synset) = buildList<Synset> {

    var parents1 = hypernyms
    var parents2 = other.hypernyms

    fun s() = parents2.forEach {
        if (it in parents1)
            add(it)
    }

    s()

    while (isEmpty()) {
        if (parents1.isEmpty() && parents2.isEmpty())
            return@buildList
        parents1 = parents1.flatMap { it.hypernyms }
        parents2 = parents2.flatMap { it.hypernyms }
        s()
    }

}

fun Synset.maxDepth(): Int {
    val parents = hypernyms
    if (hypernyms.isEmpty())
        return 0
    return 1 + parents.map { it.maxDepth() }.maxOf { it }
}

fun Dictionary.synsets(word: String): Set<Synset> {
    println("Looking up synsets for word $word")
    return listOf(POS.NOUN, POS.ADJECTIVE, POS.ADVERB, POS.VERB)
        .flatMap { getIndexWord(it, word)?.senses ?: emptyList() }
        .toSet()
}

val Synset.isRoot
    get() = hypernyms.isEmpty()

@ExperimentalStdlibApi
fun Synset.hypernymPathToRoot(): List<Synset> {
    if (hypernyms.any { it.isRoot })
        return listOf(this, hypernyms.first { it.isRoot })
    return listOf(this) + hypernymPathToRoot()
}

fun Dictionary.maxDepth() =
    listOf(POS.NOUN, POS.ADJECTIVE, POS.ADVERB, POS.VERB)
        .flatMap { getSynsetIterator(it).asSequence() }
        .map { it.maxDepth() }
        .maxOrNull()!!

fun Synset.shortestPathDistance(s2: Synset): Int? {
    var border = border
    var dist = 1
    val visited = mutableSetOf<Synset>()
    while (s2 !in border) {
        if (border.isEmpty())
            return null
        visited.addAll(border)
        border = border.asSequence().filter { it !in visited }.flatMap { it.border }.toList()
        dist++
    }
    return dist
}
