package com.github.lamba92.tln.summarization.nasari

import com.github.lamba92.tln.summarization.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import kotlin.math.min

object NasariApi {

    private const val TGZ_CHECKSUM = "5eb3f4f9ca6fcf9d3d0f2d4b782658fe"
    private const val TXT_CHECKSUM = "e910d0c05e26127949433fee4d2d8729"

    private val FILE_DATABASE_CACHE by lazy {
        Database.connect("jdbc:sqlite:NasariCache.db")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(NasariTable)
                }
            }
    }

    private val IN_MEMORY_DATABASE_CACHE by lazy {
        Database.connect("jdbc:h2:mem:NasariCache;DB_CLOSE_DELAY=-1")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(NasariTable)
                }
            }
    }

    @OptIn(KtorExperimentalAPI::class)
    suspend fun lookupArraysByLemma(lemma: String, lang: String) =
        BabelNetApi.lookupBabelSynsetsByLemma(lemma, lang)
            .mapNotNull { lookupArrayFromBabelNetId(it) }

    suspend fun lookupArrayFromBabelNetId(id: String): NasariUnifiedArray? {
        val action: suspend (Transaction.() -> NasariUnifiedArray?) = {
            NasariTable.select { NasariTable.babelNetId eq id }.map {
                NasariComparisonItem(it[NasariTable.lemma], it[NasariTable.score])
            }.let { NasariUnifiedArray(id, it) }
        }
        return defaultTransaction(IN_MEMORY_DATABASE_CACHE, action) ?: defaultTransaction(
            FILE_DATABASE_CACHE,
            action
        )?.also { array ->
            defaultTransaction(IN_MEMORY_DATABASE_CACHE) {
                array.data.forEach { element ->
                    NasariTable.insert {
                        it[babelNetId] = id
                        it[lemma] = element.lemma
                        it[score] = element.score
                    }
                }
            }
        }
    }

    private suspend fun <T> defaultTransaction(db: Database, statement: suspend Transaction.() -> T) =
        newSuspendedTransaction(
            db = db,
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            statement = statement
        )

    @OptIn(KtorExperimentalAPI::class)
    suspend fun initialize() {
        if (defaultTransaction(FILE_DATABASE_CACHE) { NasariTable.selectAll().count() != 32_146_863L }) {
            File("NasariCache.db").delete()
            val txt = retrieveNasariUnifiedData()
            defaultTransaction(FILE_DATABASE_CACHE) {
                txt.forEachLine { line ->
                    val splitLine = line.split(";")
                    val babelId = splitLine.first()
                    splitLine.drop(2)
                        .filter { it.isNotEmpty() }
                        .map { it.split("_") }
                        .filter { it.size > 1 }
                        .map { (name, rawScore) ->
                            NasariTable.insert {
                                it[babelNetId] = babelId
                                it[lemma] = name.take(15)
                                it[score] = rawScore.toDouble()
                            }
                        }

                }
            }
        }
    }

    private suspend fun downloadNasari(): File {
        val file = File("NASARI_unified.tgz")
        if (!file.exists() || file.md5Hex() != TGZ_CHECKSUM) {
            val httpClient = HttpClient(CIO)
            val url = "http://www.di.unito.it/~radicion/tmp2del/TLN_180430/dd-nasari.txt.tgz"
            println("Downloading database from $url")
            println("This will take a while!")
            val response =
                httpClient.get<HttpResponse>(url)
            if (!response.status.isSuccess())
                error(response)
            response.content.copyAndClose(file.apply {
                delete()
                createNewFile()
            }.writeChannel())
            httpClient.close()
            assert(file.md5Hex() != TGZ_CHECKSUM) {
                "${file.absolutePath} checksum ${file.md5Hex()} do not match $TGZ_CHECKSUM"
            }
        }
        return file
    }

    @KtorExperimentalAPI
    private suspend fun retrieveNasariUnifiedData(): File {
        val file = File("NASARI_unified.txt")
        if (file.exists().not() || file.md5Hex() != TXT_CHECKSUM)
            downloadNasari()
                .inputStream()
                .buffered()
                .letWithContext(Dispatchers.IO) { GzipCompressorInputStream(it) }
                .letWithContext(Dispatchers.IO) { TarArchiveInputStream(it) }
                .let {
                    val tarEntry = it.nextTarEntry
                    if (tarEntry.name != "dd-nasari.txt")
                        error("Archive is invalid. dd-nasaty.txt not found in root.")
                    file.delete()
                    file.createNewFile()
                    file.outputStream().use { writer ->
                        while (it.bytesRead < tarEntry.size) {
                            val byteBuffer = ByteArray(min(tarEntry.size - it.bytesRead, 1024).toInt())
                            it.read(byteBuffer, 0, byteBuffer.size)
                            writer.write(byteBuffer)
                        }
                        writer.flush()
                    }
                }
        if (file.md5Hex() != TXT_CHECKSUM)
            error("${file.absolutePath} checksum do not match $TXT_CHECKSUM")
        return file
    }

}
