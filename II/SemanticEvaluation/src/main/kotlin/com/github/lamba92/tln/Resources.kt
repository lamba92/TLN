package com.github.lamba92.tln

import it.lamba.utils.getResource
import net.sf.extjwnl.data.POS

@ExperimentalStdlibApi
object Resources {

    private fun String.parseNasariLine() =
        drop(3).take(9) to substringAfter('\t')


    val MINI_NASARI: MiniNasari
        get() = buildMap {
            getResource("mini_nasari/mini_nasari.tsv").forEachLine {
                val (id, array) = it.parseNasariLine()
                put(
                    "bn:$id",
                    array.split("\t").map { it.toDouble() }.toDoubleArray()
                )
            }
        }

    private val ANNOTATIONS_REGEX by lazy {
        Regex("(.*)\\t(.*)\\t+(.*)")
    }

    fun getAnnotatedPairs(path: String, fromLine: Int = 201, items: Int = 100) =
        getResource(path).readLines()
            .drop(fromLine - 1)
            .take(items)
            .map { ANNOTATIONS_REGEX.find(it)!!.destructured }
            .map { (w1, w2, score) ->
                ManualAnnotation(
                    w1.replace("_", " "),
                    w2.replace("_", " "),
                    score.toFloat()
                )
            }

    val SEMVAL_SENSES
        get() = buildMap<String, List<BabelNetSynsetId>> {
            var key = ""
            var accumulationList: MutableList<BabelNetSynsetId> = mutableListOf()
            val regex = Regex("bn:(\\d*)(\\w)")
            getResource("mini_nasari/SemEval17_IT_senses2synsets.txt").forEachLine {
                if (it.startsWith("#")) {
                    if (key != "" && accumulationList.isNotEmpty())
                        put(key, accumulationList.toList())
                    key = it.removePrefix("#")
                    accumulationList = mutableListOf()
                } else {
                    val (_, id, pos) = regex.find(it)!!.destructured
                    accumulationList.add(BabelNetSynsetId(id.toLong(), POS.getPOSForLabel(pos)))
                }
            }
        }

}


