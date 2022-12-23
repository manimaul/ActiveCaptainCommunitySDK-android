package com.garmin.marine.activecaptaincommunitysdk.api

import com.garmin.marine.activecaptaincommunitysdk.DTO.SearchMarker
import kotlinx.coroutines.flow.Flow

interface ActiveCaptain {

    fun updateDatabase(): Flow<Float>
    suspend fun eraseDatabase()
    fun queryPoints(bounds: LatLngBox): Flow<SearchMarker>
    suspend fun queryPointsSync(bounds: LatLngBox, limit: Int): List<SearchMarker>
//    //  fun queryPointsSync(bounds: LatLngBox, buffer: Array<SimplePoi?>, skipper: Skipper? = null): Int
//    fun queryPoi(id: Long): Maybe<AccPoint>
//    fun queryPoiByType(type: PoiType): Flowable<AccPoint>
//    fun queryReview(id: Long): Maybe<List<AccReview>>
//    fun getDatabaseLastUpdate(): Date?
//    fun queryLastPoiUpdate(): Single<Date>
//    fun queryLastReviewUpdate(): Single<Date>
//    fun queryNumPoiRecords(): Single<Long>
//    fun queryNumReviewRecords(): Single<Long>
//
//    fun login(fragmentManager: FragmentManager): Single<Boolean>
//    fun logout(): Completable
//    fun isLoggedIn(): Single<Boolean>
//    fun submitReview(fragmentManager: FragmentManager, id: Long, note: AccReviewRequest) : Single<AccReview>

    companion object {
//        fun initialize() : ActiveCaptain {
//
//        }
    }
}

data class LatLngBox(
    var leftLng: Double,   // left of screen
    var topLat: Double,    // top of screen
    var rightLng: Double,  // right of screen
    var bottomLat: Double, // bottom of screen
    var centerLng: Double  // center of screen
)


