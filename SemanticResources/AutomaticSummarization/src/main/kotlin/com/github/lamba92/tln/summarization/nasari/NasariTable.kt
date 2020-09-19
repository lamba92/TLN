package com.github.lamba92.tln.summarization.nasari

import org.jetbrains.exposed.dao.id.LongIdTable

object NasariTable : LongIdTable("babel_net") {
    val babelNetId = varchar("babel_net_id", 12)
    val lemma = varchar("lemma", 15)
    val score = double("score")
}
