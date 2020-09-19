package com.github.lamba92.tln.summarization

import io.ktor.util.*
import it.lamba.utils.getResource


@KtorExperimentalAPI
object Resources {

    val STOPWORDS by lazy {
        getResource("stopwords.txt")
            .readLines()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    object Corpus {
        val ANDY_WARHOL by lazy {
            getResource("corpus/Andy-Warhol.txt")
                .let { parseDocument(it) }
        }
        val EBOLA_VIRUS by lazy {
            getResource("corpus/Ebola-virus-disease.txt")
                .let { parseDocument(it) }
        }
        val LIFE_INDOORS by lazy {
            getResource("corpus/Life-indoors.txt")
                .let { parseDocument(it) }
        }
        val NAPOLEON by lazy {
            getResource("corpus/Napoleon-wiki.txt")
                .let { parseDocument(it) }
        }
        val ALL
            get() = listOf(ANDY_WARHOL, EBOLA_VIRUS, LIFE_INDOORS, NAPOLEON)
    }
}

