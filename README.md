# TLN projects

 - SECTION I - [Language Transfer](#section-i---transfer-translation)
   - [Introduction](#introduction)
   - [Grammar](#grammar)
   - [Code](#code)
     - [Data structures](#data-structures)
     - [Binary Tree](#binary-tree)
     - [ChomskyMatrix](#chomskymatrix)
     - [ckyParse](#ckyparse)
   - [Translation](#translation)
   - [Visualization](#visualization)
   - [Utilities](#utilities)
 - SECTION II - coming soon
 - SECTION III - [Document clustering](https://drive.google.com/open?id=1I-VFVcpOMebVGQ0c5FC2mYaoGEOu5XuR)

## Section I - Transfer translation

### Introduction

The scope of this project is to be able to translate a set of sentences from Italian to Yoddish.

It has been achieved using the CKY algorithm to identify all possible grammatical representations as constituent trees. 

The entire project has been written from scratch in Kotlin. Why Kotlin? Check it out the essay for this other [project](https://github.com/lamba92/Projector) of mine available [here](https://1drv.ms/b/s!Ar6fi5PcyoeolvRL7v5hdIMyQnCC6A)!

### Grammar

The POS used is a subset of [_Universal POS Tags_](https://www.sketchengine.eu/universal-pos-tags/) made available from Google. 

The subset of POS has been chosen by analyzing the sentences taken into account and only for the available words has been created a terminal right hand side. The rules available have been built by Googling around how the Italian grammar is structured. 

The grammar is in CNF, that is the [_Chomsky Normal Form_](https://en.wikipedia.org/wiki/Chomsky_normal_form), where the left side is exactly one _non terminal_ and the right side is either a single _terminal_ or exactly 2 _non terminals_.

You can check out the grammar file [here](https://github.com/lamba92/TLN/blob/master/I/src/main/resources/grammar.cfg)!

### Code

#### Data structures
 
Now, let's walk through the basic data structures that has been used:
```kotlin
data class ChomskyNormalFormGrammarRule(val lhs: GrammarElement, val rhs: RHS)

sealed class RHS {
    data class Terminal(val element: GrammarElement) : RHS()
    data class NonTerminals(val element1: GrammarElement, val element2: GrammarElement) : RHS()
}

inline class GrammarElement(val literal: String)

data class Grammar(val grammarRules: Set<ChomskyNormalFormGrammarRule>)
```
Here we have some basic structures: 
 - `GrammarElement` is the basis of the grammar framework, it's only property is the `literal` that unambiguously identifies it;
  - `ChomskyNormalFormGrammarRule` holds a Chomsky's normal form rule (ie. `A -> BC` or `A -> 'Skywalker'`); it is composed by two components: _left hand side_ and _right hand side_ where the first is a `GrammarElement` while the second can be a `RHS.Terminal` or a `RHS.NonTerminals`;
  - `RHS` is a [`sealed`](https://kotlinlang.org/docs/reference/sealed-classes.html) class that can only be either:
    - `Terminal`: a grammar element from which no other rule can follow;
    - `NonTerminals`: pair of grammar elements whereas from each a rule will be trailed around with;
 - `Grammar`: simple class holding a set of grammar rules;

#### Binary Tree

```kotlin
class Tree<K>(val root: Node<K>) : Iterable<Tree.Node<K>> {
    override fun iterator() = iterator {
        val list = mutableListOf<Node<K>>()
        yield(root)
        root.leftChild?.let { list.add(it) }
        root.rightChild?.let { list.add(it) }
        while (list.isNotEmpty()) {
            val e = list.removeAt(0)
            e.leftChild?.let { list.add(it) }
            e.rightChild?.let { list.add(it) }
            yield(e)
        }
    }

    data class Node<K>(val element: K, var leftChild: Node<K>? = null, var rightChild: Node<K>? = null)
}
```
`BinaryTree`, [as the name suggests](https://i.imgflip.com/2wkb0y.jpg), is a simple binary tree where it is possible to iterate on all nodes using depth-left-first. 

#### ChomskyMatrix

The code here is a bit longer so i'll just [link it](https://github.com/lamba92/TLN/blob/master/I/src/main/kotlin/com/github/lamba92/tln/ChomskyMatrix.kt).

It has been implemented using an array of array which is spanned using the CKY algorithm. It takes as an input a grammar and the list of words of the sentence and feeds them to the `ckyParse` function which in return fills up the matrix and builds all the possible trees. 

It exposes the root trees built with the synthetic property [`rootTrees`](https://github.com/lamba92/TLN/blob/master/I/src/main/kotlin/com/github/lamba92/tln/ChomskyMatrix.kt#L22-L23).

#### ckyParse

It is the core of the `ChomskyMatrix` class. It fills up the Chomsky Matrix from the given grammar and sentence with binary trees built using a bottom-up approach.
If the input sentence is grammatically correct in the sense of the given grammar, the algorithm will put in the top right most cell of the matrix all the possible grammatically correct trees which will be accessible using `rootTrees`. 

NB: the top-right-most cell of the matrix contains not only the starting trees. When using `rootTrees` property, they will be filtered by root `literal == "S"`  

## Translation
The approach used is _Language Transfer_. Both, input and target language have been analyzed with the goal of finding some translation rules that allow translation on the sample sentences.
Those rules have to manipulate the tree built with the ckyParse on the sentence in the input language so that it is grammatically correct in the output language.

NB: Yoddish language is a particular case of translation since it uses the same vocabulary of the input language. In a real language to language translation the vocabulary and the syntax must be taken into account.

For each sample sentence its constituent tree has been analyzed to find the proper set of transformations to apply on it in order to achieve the translation:
```kotlin
val translationMap = mapOf(
    "Tu hai molto da apprendere ancora" to listOf(GrammarElement("ADJP")),
    "Il futuro di questo ragazzo è nebuloso" to listOf(GrammarElement("ADJ")),
    "Skywalker corre velocemente" to listOf(GrammarElement("ADV")),
    "Tu hai amici lì" to listOf(GrammarElement("ADVP")),
    "Tu avrai novecento anni di età" to listOf(GrammarElement("ADJP")),
    "Noi siamo illuminati" to listOf(GrammarElement("VP"), GrammarElement("VBN"))
)
```
This map is the the output of the process above written. The translation algorithm iterate over the nodes of the built tree (depth-left-first order) and as soon as it finds the corresponding grammar element of the sentence in the map, detaches the entire subtree from there, creates a new root and puts the detached subtree as left child and the previous (now pruned) tree as right child. 

The algorithm iterates on every grammar element of the sentence and after the transformation per each element it starts over using the new tree.

The implementation is far more readable then the description itself, have a look [here](https://github.com/lamba92/TLN/blob/master/I/src/main/kotlin/com/github/lamba92/tln/Utils.kt#L92-L117)!

### Visualization
Unfortunately, I was not able to find a proper solution in JVM to easily visualize a binary tree. At the moment, [GraphStream](http://graphstream-project.org/) has been used to visualize constituent trees, but the library is more suitable for visualizing graphs. The consequence is that the draw is misplaced (even though the edges are directed correctly) and the information about left or right child is lost. Nonetheless it is possible to understand the results.

### Utilities
Since I did not rely on external libraries, mostly due to poor or inexistent tutorials on this use cases for the JVM environment and/or poor documentation for every NLP library out there, [I decided to write a parser for a grammar `.cgf` file](https://github.com/lamba92/TLN/blob/master/I/src/main/kotlin/com/github/lamba92/tln/Utils.kt#L11-L43). 

Also since I've use a custom implementation of a tree, to visualize it I [converted](https://github.com/lamba92/TLN/blob/master/I/src/main/kotlin/com/github/lamba92/tln/Utils.kt#L45-L81) it to a direct `Graph`.