package com.github.lamba92.tln.summarization

import it.lamba.utils.getResource

fun main() {
    val grammar = buildGrammarFromFile(getResource("grammar.cfg"))
    translationMap.entries.toList()[5].let { (sentence, tags) ->
        ChomskyMatrix(grammar, sentence.split(" ")).rootTrees.first()
            .apply {
                plot()
                translate(tags).plot()
            }
    }
}
