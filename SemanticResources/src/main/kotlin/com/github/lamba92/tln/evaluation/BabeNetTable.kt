package com.github.lamba92.tln.evaluation

import org.jetbrains.exposed.sql.Table

object BabeNetTable : Table("babel_net") {

    val word = varchar("word", 255)
    val babelNetId = varchar("babel_net_id", 15)

    override val primaryKey = PrimaryKey(word, babelNetId)

}
