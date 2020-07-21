package com.github.lamba92.tln

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import it.lamba.utils.getResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import kotlin.math.min

@KtorExperimentalAPI
@ExperimentalStdlibApi
object Resources {

    suspend fun getNasariUnified(): NasariUnified = buildMap {
        retrieveNasariUnifiedData().forEachLine { line ->
            val splitLine = line.split(";")
            val babelId = splitLine.first()
            val scores = splitLine.drop(2)
                .asSequence()
                .filter { it.isNotEmpty() }
                .map { it.split("_") }
                .filter { it.size > 1 }
                .map { (name, rawScore) -> NasariComparisonItem(name, rawScore.toDouble()) }
                .toList()
            put(babelId, NasariUnifiedElement(babelId, scores))
        }
    }

    private suspend fun downloadNasari(): File {
        val file = File("NASARI_unified.tgz")
        if (!file.exists() || file.length().toDouble() / (1024 * 1024) <= 250) {
            val httpClient = HttpClient(CIO)
            val response = httpClient.get<HttpResponse>(
                "http://www.di.unito.it/~radicion/tmp2del/TLN_180430/dd-nasari.txt.tgz"
            )
            if (!response.status.isSuccess())
                throw error(response)
            response.content.copyAndClose(file.apply {
                delete()
                createNewFile()
            }.writeChannel())
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

    suspend fun retrieveNasariUnifiedData(): File {
        val file = File("NASARI_unified.txt")
        if (file.exists().not() || file.length() <= 550 * 1024 * 1024)
            downloadNasari()
                .inputStream()
                .buffered()
                .letWithContext(Dispatchers.IO) {
                    GzipCompressorInputStream(it)
                }
                .letWithContext(Dispatchers.IO) {
                    TarArchiveInputStream(it)
                }
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
        return file
    }

    val STOPWORDS by lazy {
        getResource("stopwords.txt")
            .readLines()
    }

    object Corpus {
        val ANDY_WARHOL by lazy {
            getResource("corpus/Andy-Warhol.txt")
                .let { parseDocument(it) }
        }
        val EBOLA_VIRUS by lazy {
            getResource("corpus/Ebola-virus-disease.txt")
                .let { parseDocument(it) }
        }
        val LIFE_INDOORS by lazy {
            getResource("corpus/Life-indoors.txt")
                .let { parseDocument(it) }
        }
        val NAPOLEON by lazy {
            getResource("corpus/Napoleon-wiki.txt")
                .let { parseDocument(it) }
        }
        val ALL
            get() = listOf(ANDY_WARHOL, EBOLA_VIRUS, LIFE_INDOORS, NAPOLEON)
    }
}

