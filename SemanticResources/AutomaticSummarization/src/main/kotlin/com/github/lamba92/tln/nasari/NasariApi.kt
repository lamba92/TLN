package com.github.lamba92.tln.nasari

import com.github.lamba92.tln.summarization.BabelNetApi
import com.github.lamba92.tln.summarization.NasariComparisonItem
import com.github.lamba92.tln.summarization.NasariUnifiedArray
import com.github.lamba92.tln.summarization.md5
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import kotlin.math.min

suspend fun main() {
    NasariApi.buildFileDatabase()
}

object NasariApi {

    private const val TGZ_CHECKSUM = "5eb3f4f9ca6fcf9d3d0f2d4b782658fe"
    private const val TXT_CHECKSUM = "e910d0c05e26127949433fee4d2d8729"

    private val FILE_DATABASE_CACHE by lazy {
        Database.connect("jdbc:sqlite:NasariCache.db")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(NasariTable, NasariArrayTable)
                }
            }
    }

    private val IN_MEMORY_DATABASE_CACHE by lazy {
        Database.connect("jdbc:h2:mem:NasariCache;DB_CLOSE_DELAY=-1")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(NasariTable, NasariArrayTable)
                }
            }
    }

    @OptIn(KtorExperimentalAPI::class)
    suspend fun lookupArraysByLemma(lemma: String, lang: String) =
        BabelNetApi.lookupBabelSynsetsByLemma(lemma, lang)
            .map { lookupArrayFromBabelNetId(it) }

    suspend fun lookupArrayFromBabelNetId(id: String) =
        newSuspendedTransaction(
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            db = IN_MEMORY_DATABASE_CACHE
        ) {
            NasariTable.select { NasariTable.babelNetId eq id }.singleOrNull()
                ?.get(NasariTable.lemma)
                ?.let {
                    val scores = NasariArrayTable.select { NasariArrayTable.babelNetId eq id }
                        .map { NasariComparisonItem(it[NasariArrayTable.lemma], it[NasariArrayTable.score]) }
                    NasariUnifiedArray(id, it, scores)
                }
        } ?: newSuspendedTransaction(
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            db = FILE_DATABASE_CACHE
        ) {
            NasariTable.select { NasariTable.babelNetId eq id }.single()[NasariTable.lemma].let {
                val scores = NasariArrayTable.select { NasariArrayTable.babelNetId eq id }
                    .map { NasariComparisonItem(it[NasariArrayTable.lemma], it[NasariArrayTable.score]) }
                NasariUnifiedArray(id, it, scores)
            }
        }.also { array ->
            newSuspendedTransaction(
                transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
                db = IN_MEMORY_DATABASE_CACHE
            ) {
                NasariTable.insert {
                    it[babelNetId] = id
                    it[lemma] = array.lemma
                }
                array.data.forEach { element ->
                    NasariArrayTable.insert {
                        it[lemma] = element.lemma
                        it[babelNetId] = id
                        it[score] = element.score
                    }
                }
            }
        }

    @OptIn(KtorExperimentalAPI::class)
    suspend fun buildFileDatabase() {
        newSuspendedTransaction(
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            db = FILE_DATABASE_CACHE
        ) {
            retrieveNasariUnifiedData().forEachLine { line ->
                val splitLine = line.split(";")
                val babelId = splitLine.first()
                splitLine.drop(2)
                    .filter { it.isNotEmpty() }
                    .map { it.split("_") }
                    .filter { it.size > 1 }
                    .map { (name, rawScore) ->
                        NasariArrayTable.insert {
                            it[lemma] = name.take(15)
                            it[babelNetId] = babelId
                            it[score] = rawScore.toDouble()
                        }
                    }
                NasariTable.insert {
                    it[babelNetId] = babelId
                    it[lemma] = splitLine[1].take(15)
                }
            }
        }
    }

    private suspend fun downloadNasari(): File {
        val file = File("NASARI_unified.tgz")
        if (!file.exists() || file.md5() != TGZ_CHECKSUM) {
            val httpClient = HttpClient(CIO)
            val response =
                httpClient.get<HttpResponse>("http://www.di.unito.it/~radicion/tmp2del/TLN_180430/dd-nasari.txt.tgz")
            if (!response.status.isSuccess())
                error(response)
            response.content.copyAndClose(file.apply {
                delete()
                createNewFile()
            }.writeChannel())
            httpClient.close()
            if (file.md5() != TGZ_CHECKSUM)
                error("${file.absolutePath} checksum do not match $TGZ_CHECKSUM")
        }
        return file
    }

    private suspend inline fun <T, R> T.letWithContext(
        dispatcher: CoroutineDispatcher, crossinline block: (T) -> R
    ) = let {
        withContext(dispatcher) {
            block(it)
        }
    }

    @KtorExperimentalAPI
    private suspend fun retrieveNasariUnifiedData(): File {
        val file = File("NASARI_unified.txt")
        if (file.exists().not() || file.md5() != TXT_CHECKSUM)
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
        if (file.md5() != TXT_CHECKSUM)
            error("${file.absolutePath} checksum do not match $TXT_CHECKSUM")
        return file
    }
}
