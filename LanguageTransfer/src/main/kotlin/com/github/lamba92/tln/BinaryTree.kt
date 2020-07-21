package com.github.lamba92.tln

class BinaryTree<K>(val root: Node<K>) : Iterable<BinaryTree.Node<K>> {
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
