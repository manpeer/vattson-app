package ee.vattson.data.api

import ee.vattson.data.model.EleringPriceResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface EleringApi {
    /**
     * @param start ISO 8601 start time (e.g. "2026-07-20T00:00:00.000Z")
     * @param end ISO 8601 end time
     */
    @GET("api/nps/price")
    suspend fun getPrices(
        @Query("start") start: String,
        @Query("end") end: String
    ): EleringPriceResponse
}
