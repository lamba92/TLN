package com.github.lamba92.tln.evaluation

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
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
        }
    }

    private val FILE_DATABASE_CACHE by lazy {
        Database.connect("jdbc:sqlite:BabelNetCache.db")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(BabeNetTable)
                }
            }
    }

    private val IN_MEMORY_DATABASE_CACHE by lazy {
        Database.connect("jdbc:h2:mem:BabelNetCache;DB_CLOSE_DELAY=-1")
            .also {
                transaction(Connection.TRANSACTION_SERIALIZABLE, 1, it) {
                    SchemaUtils.createMissingTablesAndColumns(BabeNetTable)
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
                LemmaLookupResult.NotFoundGlobally -> emptyList()
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
            when (val data = httpCallOrNull(lemma, lang, CURRENT_API_KEY)) {
                LemmaLookupResult.ApiKeyLimitReached -> if (BABEL_NET_API_KEYS.hasNext()) CURRENT_API_KEY =
                    BABEL_NET_API_KEYS.next()
                else
                    error("API KEYS Limits reached! Steal moar keys plox")
                else -> return data
            }

        }
    }

    @KtorExperimentalAPI
    private suspend fun httpCallOrNull(lemma: String, lang: String = "IT", apiKey: String) =
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

}
