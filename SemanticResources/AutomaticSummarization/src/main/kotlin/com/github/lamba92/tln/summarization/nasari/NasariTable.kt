package com.github.lamba92.tln.summarization.nasari

import org.jetbrains.exposed.sql.Table

object NasariTable : Table("nasari") {
    val babelNetId = varchar("babel_net_id", 12)
    val lemma = varchar("lemma", 15)
    val values = varchar("values", 680)

    override val primaryKey = PrimaryKey(babelNetId)
}
