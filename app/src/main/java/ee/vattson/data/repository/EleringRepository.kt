package ee.vattson.data.repository

import android.content.Context
import com.google.gson.Gson
import ee.vattson.data.api.EleringApi
import ee.vattson.data.model.ElectricityPrice
import ee.vattson.data.model.EleringPriceResponse
import ee.vattson.data.model.PriceResolution
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class EleringRepository(private val context: Context) {

    private val gson = Gson()

    private val api: EleringApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val httpCache = getSharedHttpCache(context)

        val cacheInterceptor = Interceptor { chain ->
            val response: Response = chain.proceed(chain.request())
            val cacheControl = response.header("Cache-Control")
            if (cacheControl == null || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                cacheControl.contains("must-revalidate") || cacheControl.contains("max-age=0")
            ) {
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=" + 5 * 60)
                    .build()
            } else {
                response
            }
        }

        val client = OkHttpClient.Builder()
            .cache(httpCache)
            .addNetworkInterceptor(cacheInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://dashboard.elering.ee/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(EleringApi::class.java)
    }

    private val cacheFile by lazy {
        File(context.cacheDir, "elering_prices_cache.json")
    }

    // ---- In-memory cache layer --------------------------------------------

    @Volatile
    private var memoryPrices: List<ElectricityPrice>? = null

    @Volatile
    private var memoryHourlyAverages: List<ElectricityPrice>? = null

    @Volatile
    private var memoryFetchedAt: Long = 0L

    // ---- In-flight dedup of concurrent network fetches ---------------------

    private val fetchMutex = Mutex()
    @Volatile private var fetchInFlight: CompletableDeferred<List<ElectricityPrice>>? = null

    suspend fun getPricesForTodayAndTomorrow(): List<ElectricityPrice> {
        val (deferred, isOwner) = fetchMutex.withLock {
            val current = fetchInFlight
            if (current != null && !current.isCompleted) {
                Pair(current, false)
            } else {
                val newDeferred = CompletableDeferred<List<ElectricityPrice>>()
                fetchInFlight = newDeferred
                Pair(newDeferred, true)
            }
        }

        return if (isOwner) {
            try {
                val result = doFetch()
                deferred.complete(result)
                memoryPrices = result
                memoryHourlyAverages = null
                memoryFetchedAt = System.currentTimeMillis()
                result
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                fetchMutex.withLock {
                    if (fetchInFlight === deferred) fetchInFlight = null
                }
            }
        } else {
            deferred.await()
        }
    }

    private suspend fun doFetch(): List<ElectricityPrice> {
        val localZone = ZoneId.systemDefault()
        val start = LocalDate.now(localZone).atStartOfDay(localZone).toOffsetDateTime()
        val end = start.plusDays(2).minusNanos(1)

        val formatter = DateTimeFormatter.ISO_INSTANT

        val response = api.getPrices(
            start = formatter.format(start.toInstant()),
            end = formatter.format(end.toInstant())
        )

        saveToCache(response)

        return response.data.ee.map { item ->
            ElectricityPrice.fromEleringItem(item, PriceResolution.MINUTE_15)
        }
    }

    private suspend fun saveToCache(response: EleringPriceResponse) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(response)
            val existing = runCatching { cacheFile.readText() }.getOrNull()
            if (existing != json) {
                cacheFile.writeText(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedPrices(): List<ElectricityPrice> {
        memoryPrices?.let { return it }

        return try {
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val response = gson.fromJson(json, EleringPriceResponse::class.java) ?: return emptyList()

                val localZone = ZoneId.systemDefault()
                val nowLocal = LocalDate.now(localZone)
                val items = response.data.ee
                val hasToday = items.any { item ->
                    Instant.ofEpochSecond(item.timestamp)
                        .atZone(localZone)
                        .toLocalDate() == nowLocal
                }

                if (hasToday) {
                    val result = items.map { item ->
                        ElectricityPrice.fromEleringItem(item, PriceResolution.MINUTE_15)
                    }
                    memoryPrices = result
                    memoryHourlyAverages = null
                    memoryFetchedAt = System.currentTimeMillis()
                    return result
                }
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Converts 15-minute data to 1-hour data by averaging the prices.
     * Caches result in memory when called with the current memoryPrices reference.
     */
    fun getHourlyAverages(prices: List<ElectricityPrice>): List<ElectricityPrice> {
        if (prices === memoryPrices && memoryHourlyAverages != null) {
            return memoryHourlyAverages!!
        }

        val result = prices
            .groupBy { it.timestamp.truncatedTo(ChronoUnit.HOURS) }
            .map { (hourTimestamp, hourPrices) ->
                val avgEurMwh = hourPrices.map { it.priceEurMwh }.average()
                val avgWithVatEurMwh = hourPrices.map { it.priceWithVatEurMwh }.average()
                val avgCentsKwh = hourPrices.map { it.priceCentsKwh }.average()
                val avgWithVatCentsKwh = hourPrices.map { it.priceWithVatCentsKwh }.average()

                ElectricityPrice(
                    timestamp = hourTimestamp,
                    resolution = PriceResolution.HOUR_1,
                    priceEurMwh = avgEurMwh,
                    priceWithVatEurMwh = avgWithVatEurMwh,
                    priceCentsKwh = avgCentsKwh,
                    priceWithVatCentsKwh = avgWithVatCentsKwh
                )
            }.sortedBy { it.timestamp }.toList()

        if (prices === memoryPrices) {
            memoryHourlyAverages = result
        }
        return result
    }

    companion object {
        private val sharedCacheDirFiles = ConcurrentHashMap<String, Cache>()

        fun getSharedHttpCache(context: Context): Cache {
            val key = context.applicationContext.cacheDir.absolutePath
            return sharedCacheDirFiles.getOrPut(key) {
                Cache(File(context.applicationContext.cacheDir, "http_cache"), 10 * 1024 * 1024L)
            }
        }

        fun hasPriceForNow(prices: List<ElectricityPrice>, now: Instant = Instant.now()): Boolean {
            return prices.isNotEmpty() && prices.any { p ->
                val stepSeconds = if (p.resolution == PriceResolution.HOUR_1) 3600L else 900L
                val start = p.timestamp
                val end = start.plusSeconds(stepSeconds)
                !now.isBefore(start) && now.isBefore(end)
            }
        }
    }
}
