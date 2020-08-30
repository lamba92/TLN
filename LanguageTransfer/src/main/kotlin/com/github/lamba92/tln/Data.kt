package com.github.lamba92.tln

data class ChomskyNormalFormGrammarRule(val lhs: GrammarElement, val rhs: RHS)

sealed class RHS {
    data class Terminal(val element: GrammarElement) : RHS()
    data class NonTerminals(val element1: GrammarElement, val element2: GrammarElement) : RHS()
}

class GrammarElement(val literal: String)

data class Grammar(val grammarRules: Set<ChomskyNormalFormGrammarRule>) {
    override fun toString() = buildString {
        grammarRules.forEach { (lhs, rhs) ->
            appendLine(
                "${lhs.literal} -> ${
                    when (rhs) {
                        is RHS.Terminal -> "'${rhs.element.literal}'"
                        is RHS.NonTerminals -> "${rhs.element1.literal} ${rhs.element2.literal}"
                    }
                }"
            )
        }
    }
}

infix fun Grammar.filter(function: (ChomskyNormalFormGrammarRule) -> Boolean) =
    grammarRules.filter(function)
