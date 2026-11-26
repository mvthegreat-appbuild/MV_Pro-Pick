package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FeedSnapshot
import com.example.model.FeedTab
import com.example.model.TradeFeedDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stockpro_prefs")

class TradeRepository(private val context: Context) {

    companion object {
        private const val BASE_URL =
            "https://raw.githubusercontent.com/mvthegreat-appbuild/StockPro-Positional/main/"
        private const val CMP_FILE = "cmp.json"
        private val TAB_KEY = stringPreferencesKey("sp_tab")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val selectedTabFlow: Flow<FeedTab> = context.dataStore.data.map { prefs ->
        FeedTab.fromKey(prefs[TAB_KEY])
    }

    suspend fun saveSelectedTab(tab: FeedTab) {
        context.dataStore.edit { prefs ->
            prefs[TAB_KEY] = tab.key
        }
    }

    suspend fun fetchAll(): FetchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cmpMap = fetchCmpMap(now)
        val sp = fetchFeed(FeedTab.STOCKPRO, now)
        val mo = fetchFeed(FeedTab.MOTILAL, now)
        FetchResult(
            cmpMap = cmpMap,
            stockPro = sp,
            motilal = mo,
            checkedAtMillis = now
        )
    }

    fun loadCachedOnly(): FetchResult {
        val now = System.currentTimeMillis()
        val cmpMap = parseCmpMap(readLocalOrNull("cmp_cache.json"))
        val sp = readCachedOrAssetFeed(FeedTab.STOCKPRO, now)
        val mo = readCachedOrAssetFeed(FeedTab.MOTILAL, now)
        return FetchResult(
            cmpMap = cmpMap,
            stockPro = sp,
            motilal = mo,
            checkedAtMillis = now
        )
    }

    private fun fetchCmpMap(timestamp: Long): Map<String, Double> {
        val raw = httpGet("${BASE_URL}$CMP_FILE?t=$timestamp")
        if (raw != null) {
            writeCache("cmp_cache.json", raw)
            return parseCmpMap(raw)
        }
        return parseCmpMap(readLocalOrNull("cmp_cache.json"))
    }

    private fun fetchFeed(tab: FeedTab, timestamp: Long): FeedSnapshot? {
        val raw = httpGet("${BASE_URL}${tab.fileName}?t=$timestamp")
        if (raw != null) {
            val parsed = runCatching { json.decodeFromString<TradeFeedDto>(raw) }.getOrNull()
            if (parsed != null) {
                writeCache("${tab.key}_cache.json", raw)
                return FeedSnapshot(feed = parsed, isLive = true, checkedAtMillis = timestamp)
            }
        }
        return readCachedOrAssetFeed(tab, timestamp)
    }

    private fun readCachedOrAssetFeed(tab: FeedTab, timestamp: Long): FeedSnapshot? {
        val cachedRaw = readLocalOrNull("${tab.key}_cache.json")
        if (cachedRaw != null) {
            val parsed = runCatching { json.decodeFromString<TradeFeedDto>(cachedRaw) }.getOrNull()
            if (parsed != null) {
                return FeedSnapshot(feed = parsed, isLive = false, checkedAtMillis = timestamp)
            }
        }
        val assetRaw = readAssetOrNull(tab.fileName)
        if (assetRaw != null) {
            val parsed = runCatching { json.decodeFromString<TradeFeedDto>(assetRaw) }.getOrNull()
            if (parsed != null) {
                return FeedSnapshot(feed = parsed, isLive = false, checkedAtMillis = timestamp)
            }
        }
        return null
    }

    private fun parseCmpMap(raw: String?): Map<String, Double> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val pricesObj: JsonObject = root["prices"]?.jsonObject ?: root
            buildMap {
                for ((symbol, element) in pricesObj) {
                    val value = runCatching { element.jsonPrimitive.doubleOrNull }.getOrNull()
                        ?: runCatching { element.jsonPrimitive.content.toDoubleOrNull() }.getOrNull()
                    if (value != null && value.isFinite() && value > 0.0) {
                        put(symbol.trim().uppercase(), value)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun httpGet(url: String): String? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Pragma", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull()
    }

    private fun writeCache(fileName: String, content: String) {
        runCatching {
            File(context.filesDir, fileName).writeText(content)
        }
    }

    private fun readLocalOrNull(fileName: String): String? {
        return runCatching {
            val file = File(context.filesDir, fileName)
            if (file.exists()) file.readText() else null
        }.getOrNull()
    }

    private fun readAssetOrNull(fileName: String): String? {
        return runCatching {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }.getOrNull()
    }
}

data class FetchResult(
    val cmpMap: Map<String, Double>,
    val stockPro: FeedSnapshot?,
    val motilal: FeedSnapshot?,
    val checkedAtMillis: Long
)
