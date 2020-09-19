package com.github.lamba92.tln.nasari

import org.jetbrains.exposed.sql.Table

object NasariTable : Table("babel_net") {
    val babelNetId = varchar("babel_net_id", 15)
    val lemma = varchar("title", 15)

    override val primaryKey = PrimaryKey(babelNetId)
}
