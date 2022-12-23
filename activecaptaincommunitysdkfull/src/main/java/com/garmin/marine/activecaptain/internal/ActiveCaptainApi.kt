//package com.garmin.marine.activecaptain.internal
//
//import com.garmin.marine.activecaptain.internal.contract.BoundingBox
//import com.garmin.marine.activecaptain.internal.contract.TileCoordinate
//import com.garmin.marine.activecaptain.internal.contract.request.SyncStatusRequest
//import com.garmin.marine.activecaptain.internal.contract.response.GetUserResponse
//import com.garmin.marine.activecaptain.internal.contract.response.SyncStatusResponse
//import okhttp3.OkHttpClient
//import okhttp3.ResponseBody
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Response
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.*
//import java.util.concurrent.TimeUnit
//
//interface IAc2Service {
//    @GET("api/v2/authentication/access-token")
//    suspend fun getAccessToken(
//        @Query("serviceUrl") serviceUrl: String?,
//        @Query("serviceTicket") serviceTicket: String?
//    ): String
//
//    @POST("api/v2/authentication/refresh-token")
//    suspend fun getRefreshToken(@Header("Authorization") authHeader: String?): String
//
//    @POST("api/v2/points-of-interest/tiles")
//    suspend fun getTiles(@Body request: List<BoundingBox>): List<TileCoordinate>
//
//    @POST("api/v2/points-of-interest/export")
//    suspend fun getExports(@Body request: List<TileCoordinate>): Response<List<ExportResponse>>
//
//    @POST("api/v2.1/points-of-interest/sync-status")
//    suspend fun getSyncStatus(
//        @Query("databaseVersion") databaseVersion: String,
//        @Body request: List<SyncStatusRequest>
//    ): List<SyncStatusResponse>
//
//    @GET("api/v1/user")
//    suspend fun getUser(@Header("Authorization") authHeader: String?): GetUserResponse
//
//    @POST("api/v2/points-of-interest/{id}/view")
//    suspend fun reportMarkerViewed(@Path("id") id: Long): Response<Unit>
//
//    @GET("api/v2/points-of-interest/sync")
//    suspend fun syncMarkers(
//        @Query("tileX") tileX: Int,
//        @Query("tileY") tileY: Int,
//        @Query("lastModifiedAfter") lastModifiedAfter: String?
//    ): Response<ResponseBody?>
//
//    @GET("api/v2/reviews/sync")
//    suspend fun syncReviews(
//        @Query("tileX") tileX: Int,
//        @Query("tileY") tileY: Int,
//        @Query("lastModifiedAfter") lastModifiedAfter: String?
//    ): Response<ResponseBody?>
//
//    @POST("api/v2/reviews/{id}/votes")
//    suspend fun voteForReview(
//        @Path("id") id: Long,
//        @Header("Authorization") authHeader: String?
//    ): Response<Unit>
//}
//
//internal class Ac2Service {
//
//    val api: IAc2Service = Retrofit.Builder()
//        .baseUrl(ActiveCaptainConfig.API_BASE_URL)
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//        .create(IAc2Service::class.java)
//
//    companion object {
//        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
//            .addInterceptor { chain ->
//                chain.proceed(
//                    chain.request()
//                        .newBuilder()
//                        .header("apikey", ActiveCaptainConfig.API_KEY)
//                        .build()
//                )
//            }
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS).apply {
////                if (BuildConfig.DEBUG) addInterceptor(
////                    HttpLoggingInterceptor().setLevel(
////                        HttpLoggingInterceptor.Level.BASIC
////                    )
////                )
//            }
//            .build()
//    }
//}