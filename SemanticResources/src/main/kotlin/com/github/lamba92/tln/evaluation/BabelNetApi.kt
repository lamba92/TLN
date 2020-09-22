package com.github.lamba92.tln.evaluation

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

object BabelNetApi {

    @KtorExperimentalAPI
    private val HTTP_CLIENT by lazy {
        HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            install(HttpCallValidator) {
                validateResponse {
                    if (it.status != HttpStatusCode.OK) {
                        val response = it.readBytes().toString()
                        when {
                            "BabelSynset not found." in response -> throw SynsetNotFoundError()
                            "API" in response -> throw ApiKeysLimitReachedError()
                        }
                    }
                }
            }
        }
    }

    private val FILE_DATABASE_CACHE by lazy {
        Database.connect("jdbc:sqlite:BabelNetCache.db")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(BabeNetTable, BabelNetSynsetsDetails)
                }
            }
    }

    private val IN_MEMORY_DATABASE_CACHE by lazy {
        Database.connect("jdbc:h2:mem:BabelNetCache;DB_CLOSE_DELAY=-1")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(BabeNetTable, BabelNetSynsetsDetails)
                }
            }
    }

    private val BABEL_NET_API_KEYS by lazy {
        (System.getenv("BABEL_NET_API_KEY")
            ?: File("babel_net_keys.txt")
                .takeIf { it.exists() }
                ?.readText()
                ?.takeIf { it.isNotBlank() }
            ?: throw error("No BabelNetApi keys found. Please create babel_net_keys.txt in the CWD with the keys separated by semicolons or set BABEL_NET_API_KEY environment variable."))
            .split(";")
            .iterator()
    }

    private var CURRENT_API_KEY = BABEL_NET_API_KEYS.next()

    @Serializable
    private data class NetIdQuery(val id: String, val pos: String, val source: String)

    @Serializable
    data class SynsetDetailsResponse(val senses: List<Sense>) {
        @Serializable
        data class Sense(
            val type: String,
            val properties: Property
        ) {
            @Serializable
            data class Property(
                val fullLemma: String,
                val simpleLemma: String,
                val source: String,
                val senseKey: String,
                val frequency: Int,
                val language: String,
                val pos: String,
                val synsetID: SynsetId,
                val translationInfo: String,
                val pronunciations: Pronunciation,
                val bKeySense: Boolean,
                val idSense: Long,
            ) {
                @Serializable
                data class SynsetId(val id: String, val pos: String, val source: String)

                @Serializable
                data class Pronunciation(val audios: List<String>, val transcriptions: List<String>)
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun getBabelSynsetDetails(id: String): List<SynsetDetailsResponse.Sense> =
        when (val c1 = lookUpBabelSynsetDetailsInCache(id, IN_MEMORY_DATABASE_CACHE)) {
            is SynsetDetailsLookupResult.Found -> c1.data.senses
            is SynsetDetailsLookupResult.NotFoundGlobally -> emptyList()
            else -> when (val c2 = lookUpBabelSynsetDetailsInCache(id, FILE_DATABASE_CACHE)) {
                is SynsetDetailsLookupResult.Found -> {
                    cacheBabelNetSynsetsDetails(id, c2.data, IN_MEMORY_DATABASE_CACHE)
                    c2.data.senses
                }
                SynsetDetailsLookupResult.NotFoundGlobally -> {
                    cacheBabelNetSynsetsDetails(id, null, IN_MEMORY_DATABASE_CACHE)
                    emptyList()
                }
                else -> when (val net = lookUpBabelSynsetsDetailsOnline(id)) {
                    is SynsetDetailsLookupResult.Found -> {
                        cacheBabelNetSynsetsDetails(id, net.data, IN_MEMORY_DATABASE_CACHE)
                        cacheBabelNetSynsetsDetails(id, net.data, FILE_DATABASE_CACHE)
                        net.data.senses
                    }
                    else -> {
                        cacheBabelNetSynsetsDetails(id, null, IN_MEMORY_DATABASE_CACHE)
                        cacheBabelNetSynsetsDetails(id, null, FILE_DATABASE_CACHE)
                        emptyList()
                    }
                }
            }
        }

    private suspend fun cacheBabelNetSynsetsDetails(id: String, details: SynsetDetailsResponse?, db: Database): Unit =
        newSuspendedTransaction(
            db = db,
            transactionIsolation = if (db == FILE_DATABASE_CACHE) Connection.TRANSACTION_SERIALIZABLE else null
        ) {
            details?.senses?.forEach { sense ->
                BabelNetSynsetsDetails.insert {
                    it[babelNetId] = id
                    it[type] = sense.type
                    it[fullLemma] = sense.properties.fullLemma
                    it[simpleLemma] = sense.properties.simpleLemma
                    it[idSource] = sense.properties.source
                    it[senseKey] = sense.properties.senseKey
                    it[frequency] = sense.properties.frequency
                    it[language] = sense.properties.language
                    it[pos] = sense.properties.pos
                    it[bKeySense] = sense.properties.bKeySense
                    it[idSense] = sense.properties.idSense
                    it[translationInfo] = sense.properties.translationInfo
                }
            } ?: BabelNetSynsetsDetails.insert {
                it[babelNetId] = id
                it[type] = "FAKE"
                it[fullLemma] = ""
                it[simpleLemma] = ""
                it[idSource] = ""
                it[senseKey] = ""
                it[frequency] = 0
                it[language] = ""
                it[pos] = ""
                it[bKeySense] = false
                it[idSense] = 0L
                it[translationInfo] = ""
            }
        }

    private suspend fun lookUpBabelSynsetDetailsInCache(id: String, db: Database) =
        newSuspendedTransaction(
            db = db,
            transactionIsolation = if (db == FILE_DATABASE_CACHE) Connection.TRANSACTION_SERIALIZABLE else null
        ) {
            val data = BabelNetSynsetsDetails.select { BabelNetSynsetsDetails.babelNetId eq id }
                .map {
                    SynsetDetailsResponse.Sense(
                        it[BabelNetSynsetsDetails.type],
                        SynsetDetailsResponse.Sense.Property(
                            it[BabelNetSynsetsDetails.fullLemma],
                            it[BabelNetSynsetsDetails.simpleLemma],
                            it[BabelNetSynsetsDetails.idSource],
                            it[BabelNetSynsetsDetails.senseKey],
                            it[BabelNetSynsetsDetails.frequency],
                            it[BabelNetSynsetsDetails.language],
                            it[BabelNetSynsetsDetails.pos],
                            SynsetDetailsResponse.Sense.Property.SynsetId(
                                it[BabelNetSynsetsDetails.babelNetId],
                                it[BabelNetSynsetsDetails.pos],
                                it[BabelNetSynsetsDetails.idSource]
                            ),
                            it[BabelNetSynsetsDetails.translationInfo],
                            SynsetDetailsResponse.Sense.Property.Pronunciation(
                                emptyList(),
                                emptyList()
                            ),
                            it[BabelNetSynsetsDetails.bKeySense],
                            it[BabelNetSynsetsDetails.idSense]
                        )
                    )
                }
            when {
                data.any { it.type == "FAKE" } -> SynsetDetailsLookupResult.NotFoundGlobally
                data.isEmpty() -> SynsetDetailsLookupResult.NotFoundLocally
                else -> SynsetDetailsLookupResult.Found(SynsetDetailsResponse(data))
            }
        }

    @KtorExperimentalAPI
    private suspend fun lookUpBabelSynsetsDetailsOnline(id: String): SynsetDetailsLookupResult {
        while (true) {
            when (val data = synsetHttpCallOrNull(id, CURRENT_API_KEY)) {
                SynsetDetailsLookupResult.ApiKeyLimitReached -> {
                    if (BABEL_NET_API_KEYS.hasNext()) CURRENT_API_KEY =
                        BABEL_NET_API_KEYS.next()
                    else
                        error("API KEYS Limits reached! Steal moar keys plox")
                }
                else -> return data
            }
        }
    }

    @KtorExperimentalAPI
    private suspend fun synsetHttpCallOrNull(synset: String, apiKey: String) =
        runCatching {
            HTTP_CLIENT.get<SynsetDetailsResponse>("https://babelnet.io/v5/getSynset") {
                parameter("id", synset)
                parameter("key", apiKey)
            }
        }.let {
            when {
                it.isSuccess -> SynsetDetailsLookupResult.Found(it.getOrThrow())
                else -> when (val ex = it.exceptionOrNull()!!) {
                    is ApiKeysLimitReachedError -> SynsetDetailsLookupResult.ApiKeyLimitReached
                    is SynsetNotFoundError -> SynsetDetailsLookupResult.NotFoundGlobally
                    else -> throw ex
                }
            }
        }

    @KtorExperimentalAPI
    suspend fun lookupBabelSynsetsByLemma(lemma: String, lang: String = "IT") =
        when (val c1 = lookUpBabelSynsetsByLemmaInCache(lemma, IN_MEMORY_DATABASE_CACHE)) {
            is LemmaLookupResult.Found -> c1.data
            LemmaLookupResult.NotFoundGlobally -> emptyList()
            else -> when (val c2 = lookUpBabelSynsetsByLemmaInCache(lemma, FILE_DATABASE_CACHE)) {
                is LemmaLookupResult.Found -> {
                    cacheBabelNetSynsetsByLemma(lemma, c2.data, IN_MEMORY_DATABASE_CACHE)
                    c2.data
                }
                LemmaLookupResult.NotFoundGlobally -> {
                    cacheBabelNetSynsetsByLemma(lemma, null, IN_MEMORY_DATABASE_CACHE)
                    emptyList()
                }
                else -> when (val net = lookUpBabelSynsetsByLemmaOnline(lemma, lang)) {
                    is LemmaLookupResult.Found -> {
                        cacheBabelNetSynsetsByLemma(lemma, net.data, IN_MEMORY_DATABASE_CACHE)
                        cacheBabelNetSynsetsByLemma(lemma, net.data, FILE_DATABASE_CACHE)
                        net.data
                    }
                    else -> {
                        cacheBabelNetSynsetsByLemma(lemma, null, IN_MEMORY_DATABASE_CACHE)
                        cacheBabelNetSynsetsByLemma(lemma, null, FILE_DATABASE_CACHE)
                        emptyList()
                    }
                }
            }
        }

    @KtorExperimentalAPI
    private suspend fun lookUpBabelSynsetsByLemmaOnline(lemma: String, lang: String = "IT"): LemmaLookupResult {
        while (true) {
            when (val data = lemmaHttpCallOrNull(lemma, lang, CURRENT_API_KEY)) {
                LemmaLookupResult.ApiKeyLimitReached -> if (BABEL_NET_API_KEYS.hasNext()) CURRENT_API_KEY =
                    BABEL_NET_API_KEYS.next()
                else
                    error("API KEYS Limits reached! Steal moar keys plox")
                else -> return data
            }
        }
    }

    @KtorExperimentalAPI
    private suspend fun lemmaHttpCallOrNull(lemma: String, lang: String = "IT", apiKey: String) =
        runCatching {
            HTTP_CLIENT.get<List<NetIdQuery>>("https://babelnet.io/v5/getSynsetIds") {
                parameter("lemma", lemma)
                parameter("searchLang", lang)
                parameter("key", apiKey)
            }
        }.let {
            when {
                it.isSuccess -> when {
                    it.getOrThrow().isEmpty() -> LemmaLookupResult.NotFoundGlobally
                    else -> LemmaLookupResult.Found(it.getOrThrow().map { it.id })
                }
                else -> LemmaLookupResult.ApiKeyLimitReached
            }
        }

    private suspend fun cacheBabelNetSynsetsByLemma(lemma: String, synsetsIds: List<String>?, db: Database) =
        newSuspendedTransaction(
            db = db,
            transactionIsolation = if (db == FILE_DATABASE_CACHE) Connection.TRANSACTION_SERIALIZABLE else null
        ) {
            synsetsIds?.forEach { synsetId ->
                BabeNetTable.insert {
                    it[word] = lemma
                    it[babelNetId] = synsetId
                }
            } ?: BabeNetTable.insert {
                it[word] = lemma
                it[babelNetId] = ""
            }
        }

    private suspend fun lookUpBabelSynsetsByLemmaInCache(lemma: String, db: Database) =
        newSuspendedTransaction(
            db = db,
            transactionIsolation = if (db == FILE_DATABASE_CACHE) Connection.TRANSACTION_SERIALIZABLE else null
        ) {
            BabeNetTable.select { BabeNetTable.word eq lemma }
                .map { it[BabeNetTable.babelNetId] }
                .toLemmaResult()
        }

    private fun List<String>.toLemmaResult() = when {
        firstNotNull { isEmpty() } == true -> LemmaLookupResult.NotFoundGlobally
        isEmpty() -> LemmaLookupResult.NotFoundLocally
        else -> LemmaLookupResult.Found(this)
    }

    private sealed class LemmaLookupResult {
        object NotFoundGlobally : LemmaLookupResult()
        object NotFoundLocally : LemmaLookupResult()
        object ApiKeyLimitReached : LemmaLookupResult()
        data class Found(val data: List<String>) : LemmaLookupResult()
    }

    private sealed class SynsetDetailsLookupResult {
        object NotFoundGlobally : SynsetDetailsLookupResult()
        object NotFoundLocally : SynsetDetailsLookupResult()
        object ApiKeyLimitReached : SynsetDetailsLookupResult()
        data class Found(val data: SynsetDetailsResponse) : SynsetDetailsLookupResult()
    }

    private class ApiKeysLimitReachedError : Throwable()

    private class SynsetNotFoundError : Throwable()

}
