package com.github.lamba92.tln

import it.lamba.utils.getResource
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import java.io.File


internal val ruleRegex by lazy { Regex("(\\w*)\\s*->\\s*(.*)") }
internal val nonTerminalRegex by lazy { Regex("\\s*(\\w*) (\\w*)\\s*") }
internal val terminalRhsRegex by lazy { Regex("\\s*'(.*)'\\s*") }

fun buildGrammarFromFile(file: File) = file.readLines()
    .asSequence()
    .filter { !it.startsWith("#") }
    .filter { it.isNotBlank() }
    .filter { it.matches(ruleRegex) }
    .map {
        ruleRegex.find(it)!!.destructured.run {
            component1() to component2().split("|")
        }
    }
    .flatMap { (lhs, rhs) ->
        rhs.checkAndTransformRhs()
            .map { ChomskyNormalFormGrammarRule(GrammarElement(lhs), it) }
    }
    .toSet()
    .let { Grammar(it) }

internal fun List<String>.checkAndTransformRhs() = asSequence().mapNotNull {
    when {
        it.matches(terminalRhsRegex) -> RHS.Terminal(GrammarElement(terminalRhsRegex.find(it)!!.destructured.component1()))
        it.matches(nonTerminalRegex) -> it.splitNonTerminalPairs()
        else -> null
    }
}

internal fun String.splitNonTerminalPairs() =
    nonTerminalRegex.find(this)!!.destructured.let {
        RHS.NonTerminals(GrammarElement(it.component1()), GrammarElement(it.component2()))
    }

fun BinaryTree<GrammarElement>.plot() =
    plotTree(this)


fun plotTree(tree: BinaryTree<GrammarElement>) {
    val graph: Graph = SingleGraph("Tutorial 1")
    graph.addAttribute("ui.stylesheet", getResource("graph-style.css").readText())
    graph.isStrict = false
    val treeIds = tree.mapIndexed { index, node ->
        node to index.toString()
    }.toMap()

    graph.display().apply {
        enableAutoLayout()
    }

    tree.forEach { root ->
        graph.addNode<Node>(treeIds[root]).apply {
            addAttribute("ui.label", root.element.literal)
            addAttribute("ui.class", "node")
        }
        root.leftChild?.let {
            graph.addNode<Node>(treeIds[it]).apply {
                addAttribute("ui.label", it.element.literal)
                addAttribute("ui.class", "node")
            }
            graph.addEdge<Edge>(treeIds[root] + treeIds[it], treeIds[root], treeIds[it], true)
        }
        root.rightChild?.let {
            graph.addNode<Node>(treeIds[it]).apply {
                addAttribute("ui.label", it.element.literal)
                addAttribute("ui.class", "node")
            }
            graph.addEdge<Edge>(treeIds[root] + treeIds[it], treeIds[root], treeIds[it], true)
        }
    }
}

val translationMap = mapOf(
    "Tu hai molto da apprendere ancora" to listOf(GrammarElement("ADJP")),
    "Il futuro di questo ragazzo è nebuloso" to listOf(GrammarElement("ADJ")),
    "Skywalker corre velocemente" to listOf(GrammarElement("ADV")),
    "Tu hai amici lì" to listOf(GrammarElement("ADVP")),
    "Tu avrai novecento anni di età" to listOf(GrammarElement("ADJP")),
    "Noi siamo illuminati" to listOf(GrammarElement("VP"), GrammarElement("VBN"))
)

fun BinaryTree<GrammarElement>.moveLeft(element: GrammarElement) =
    first {
        it.leftChild?.element?.literal == element.literal || it.rightChild?.element?.literal == element.literal
    }
        .run {
            if (leftChild?.element?.literal == element.literal) {
                val r = leftChild!!
                leftChild = null
                r
            } else {
                val r = rightChild!!
                rightChild = null
                r
            }
        }
        .let {
            BinaryTree(BinaryTree.Node(GrammarElement("S'"), it, root))
        }

fun BinaryTree<GrammarElement>.translate(rules: List<GrammarElement>): BinaryTree<GrammarElement> {
    var tree = this
    rules.forEach {
        tree = tree.moveLeft(it)
    }
    return tree
}