package com.github.lamba92.tln.nasari

import org.jetbrains.exposed.dao.id.LongIdTable

object NasariArrayTable : LongIdTable("nasari_array") {
    val babelNetId = reference("babelNetId", NasariTable.babelNetId)
    val lemma = varchar("lemma", 15)
    val score = double("score")
}
