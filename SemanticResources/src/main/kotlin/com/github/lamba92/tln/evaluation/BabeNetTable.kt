package com.github.lamba92.tln.evaluation

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table

object BabeNetTable : Table("babel_net") {

    val word = varchar("word", 255)
    val babelNetId = varchar("babel_net_id", 15)

    override val primaryKey = PrimaryKey(word, babelNetId)

}

object BabelNetSynsetsDetails : LongIdTable("synset_details") {

    val babelNetId = varchar("babelNetId", 15)
    val type = varchar("type", 10)
    val fullLemma = varchar("fullLemma", 15)
    val simpleLemma = varchar("simpleLemma", 15)
    val idSource = varchar("idSource", 15)
    val senseKey = varchar("senseKey", 15)
    val frequency = integer("frequency")
    val language = varchar("language", 15)
    val pos = varchar("pos", 15)
    val translationInfo = varchar("translationInfo", 15)
    val bKeySense = bool("bKeySense")
    val idSense = long("idSense")

}
