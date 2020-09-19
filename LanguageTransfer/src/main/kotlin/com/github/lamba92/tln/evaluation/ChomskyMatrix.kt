package com.github.lamba92.tln.evaluation

import com.github.lamba92.tln.evaluation.BinaryTree.Node

class ChomskyMatrix(grammar: Grammar, words: List<String>) {

    constructor(grammar: Grammar, vararg words: String) : this(grammar, words.toList())

    val size = words.size

    private val table = Array(size + 1) {
        Array(size + 1) { mutableSetOf<BinaryTree<GrammarElement>>() }
    }

    init {
        ckyParse(words, grammar)
    }

    operator fun get(key1: Int, key2: Int) =
        table[key1][key2]

    val rootTrees
        get() = this[0, size].filter { it.root.element.literal == "S" }.toSet()

    private fun ckyParse(words: List<String>, grammar: Grammar) {
        for (j in 1 until size + 1) {
            this[j - 1, j] += grammar.filter { it.rhs is RHS.Terminal && it.rhs.element.literal == words[j - 1] }
                .map {
                    BinaryTree(
                        Node(
                            it.lhs,
                            Node((it.rhs as RHS.Terminal).element)
                        )
                    )
                }

            for (i in j - 1 downTo 0) for (k in i + 1 until j) {
                this[i, j] += grammar.withExactlyTwoNonTerminalsRhs(this[i, k], this[k, j])
            }
        }
    }

    private infix fun GrammarElement.belongsTo(other: Set<BinaryTree<GrammarElement>>) =
        other.any { it.root.element == this }

    private infix fun GrammarElement.inRootOf(other: Set<BinaryTree<GrammarElement>>) =
        other.filter { it.root.element == this }

    private infix fun GrammarElement.firstInRootOf(other: Set<BinaryTree<GrammarElement>>) =
        inRootOf(other).first()

    private fun Grammar.withExactlyTwoNonTerminalsRhs(
        b: Set<BinaryTree<GrammarElement>>,
        c: Set<BinaryTree<GrammarElement>>
    ) = grammarRules
        .asSequence()
        .filter { (_, rhs) ->
            rhs is RHS.NonTerminals && rhs.element1 belongsTo b && rhs.element2 belongsTo c
        }
        .map { (lhs, rhs) ->
            require(rhs is RHS.NonTerminals)
            BinaryTree(
                Node(
                    lhs,
                    rhs.element1.firstInRootOf(b).root,
                    rhs.element2.firstInRootOf(c).root
                )
            )
        }
        .toList()

}
