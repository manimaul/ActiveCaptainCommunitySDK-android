/*------------------------------------------------------------------------------
Copyright 2021 Garmin Ltd. or its subsidiaries.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
------------------------------------------------------------------------------*/
package com.garmin.marine.activecaptain

import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.text.format.DateUtils
import android.util.Log
import com.garmin.marine.activecaptain.internal.ActiveCaptainConfiguration
import com.garmin.marine.activecaptain.internal.ExportDownloader
import com.garmin.marine.activecaptain.internal.contract.BoundingBox
import com.garmin.marine.activecaptain.internal.contract.TileCoordinate
import com.garmin.marine.activecaptain.internal.contract.request.SyncStatusRequest
import com.garmin.marine.activecaptain.internal.contract.response.SyncStatusResponse.SyncStatusType
import com.garmin.marine.activecaptaincommunitysdk.ActiveCaptainDatabase
import com.garmin.marine.activecaptaincommunitysdk.DTO.LastUpdateInfoType
import com.garmin.marine.activecaptaincommunitysdk.DTO.TileXY
import java.io.File
import java.io.IOException
import java.util.*

class ActiveCaptainManager internal constructor(val config: ActiveCaptainConfiguration) {

    private val apiInterface: ActiveCaptainApiInterface
    private var captainName: String?
    val database: ActiveCaptainDatabase
    private val exportDownloader: ExportDownloader
    private var boundingBoxes: List<BoundingBox>
    private var updateTask: Runnable
    private val updateHandler: Handler

    private enum class SyncResult {
        SUCCESS, FAIL, EXPORT_REQUIRED
    }

    init {
        requireNotNull(basePath) { "basePath must not be null." }
        requireNotNull(sharedPreferences) { "sharedPreferences must not be null." }
        database = ActiveCaptainDatabase(File(basePath, "active_captain.db"), config.languageCode)
        apiInterface = ActiveCaptainApiClient.getClient(config).create(
            ActiveCaptainApiInterface::class.java
        )
        boundingBoxes = LinkedList()
        exportDownloader = ExportDownloader(database, basePath)
        captainName = null
        val updateThread = HandlerThread("UpdateThread")
        updateThread.start()
        updateHandler = Handler(updateThread.looper)
        updateTask = Runnable {
            updateData()
            run {
                updateHandler.postDelayed(
                    updateTask,
                    config.updateIntervalMinutes * DateUtils.MINUTE_IN_MILLIS
                )
            }
        }
    }

    fun getAccessToken(serviceUrl: String?, serviceTicket: String?) {
        val call = apiInterface.getAccessToken(serviceUrl, serviceTicket)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                jwt = response.body()
            } else {
                Log.e(
                    "Error: ",
                    "Failed to get access token, " + response.code() + " " + response.body()
                )
            }
        } catch (e: IOException) {
            Log.e("Error: ", "Failed to get access token, " + e.message)
        }
    }

    fun getCaptainName(): String? {
        //todo: network on main thread exception
        if (captainName == null && jwt != null) {
            val call = apiInterface.getUser("Bearer $jwt")
            try {
                val response = call.execute()
                if (response.isSuccessful && response.body() != null) {
                    captainName = response.body()!!.CaptainName
                }
            } catch (e: IOException) {
                Log.e("Error: ", "Failed to get user, " + e.message)
            }
        }
        return captainName
    }

    var jwt: String?
        get() = sharedPreferences!!.getString(JWT_KEY, null)
        set(jwt) {
            sharedPreferences!!.edit().putString(JWT_KEY, jwt).apply()
        }

    fun refreshToken() {
        val call = apiInterface.getRefreshToken("Bearer $jwt")
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                jwt = response.body()
            } else {
                Log.e(
                    "Error: ",
                    "Failed to get access token, " + response.code() + " " + response.body()
                )
            }
        } catch (e: IOException) {
            Log.e("Error: ", "Failed to get access token, " + e.message)
        }
    }

    fun reportMarkerViewed(markerId: Long) {
        val call = apiInterface.reportMarkerViewed(markerId)
        try {
            call.execute()
            // If this call fails, we are not required to retry or queue for later.
        } catch (e: IOException) {
            Log.e("Error: ", "Failed to report marker viewed, " + e.message)
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        if (enabled) {
            updateTask.run()
        } else {
            updateHandler.removeCallbacks(updateTask)
        }
    }

    fun setBoundingBoxes(boundingBoxes: List<BoundingBox>) {
        this.boundingBoxes = boundingBoxes
    }

    fun updateData() {
        Log.d("ActiveCaptainManager", "UpdateData called")
        if (boundingBoxes.isEmpty()) {
            return
        }
        val lastUpdateInfos = HashMap<TileXY, LastUpdateInfoType>()
        for (boundingBox in boundingBoxes) {
            val bboxLastUpdateInfos = database.getTilesLastModifiedByBoundingBox(
                boundingBox.southwestCorner.latitude,
                boundingBox.southwestCorner.longitude,
                boundingBox.northeastCorner.latitude,
                boundingBox.northeastCorner.longitude
            )
            if (bboxLastUpdateInfos != null) {
                lastUpdateInfos.putAll(bboxLastUpdateInfos)
            }
        }
        val tileRequests: MutableList<SyncStatusRequest> = LinkedList()
        if (lastUpdateInfos.isEmpty()) {
            // Database not present, need to get tiles from API.
            val tileCall = apiInterface.getTiles(boundingBoxes)
            try {
                val response = tileCall.execute()
                if (response.isSuccessful && response.body() != null) {
                    for (tileCoordinate in response.body()!!) {
                        tileRequests.add(
                            SyncStatusRequest(
                                tileCoordinate.tileX,
                                tileCoordinate.tileY,
                                null,
                                null
                            )
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e("Error: ", "Failed to get tiles for bounding box, " + e.message)
            }
        } else {
            for ((key, value) in lastUpdateInfos) {
                tileRequests.add(
                    SyncStatusRequest(
                        key.tileX,
                        key.tileY,
                        value.markerLastUpdate,
                        value.reviewLastUpdate
                    )
                )
            }
        }
        val exportTileList: MutableSet<TileCoordinate> = HashSet()
        val call = apiInterface.getSyncStatus(database.version, tileRequests)
        try {
            val response = call.execute()
            if (response.isSuccessful && response.body() != null) {
                for (tileResponse in response.body()!!) {
                    val tileCoordinate = TileCoordinate(tileResponse.TileX, tileResponse.TileY)
                    when (tileResponse.PoiUpdateType) {
                        SyncStatusType.Sync -> if (syncTileMarkers(tileCoordinate) == SyncResult.EXPORT_REQUIRED) {
                            exportTileList.add(tileCoordinate)
                        }
                        SyncStatusType.Export -> exportTileList.add(tileCoordinate)
                        SyncStatusType.Delete -> database.deleteTile(
                            tileCoordinate.tileX,
                            tileCoordinate.tileY
                        )
                        SyncStatusType.None, null -> {}
                    }
                    when (tileResponse.ReviewUpdateType) {
                        SyncStatusType.Sync -> if (syncTileReviews(tileCoordinate) == SyncResult.EXPORT_REQUIRED) {
                            exportTileList.add(tileCoordinate)
                        }
                        SyncStatusType.Export -> exportTileList.add(tileCoordinate)
                        SyncStatusType.Delete -> database.deleteTileReviews(
                            tileCoordinate.tileX,
                            tileCoordinate.tileY
                        )
                        SyncStatusType.None, null -> {}
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("Error: ", "Failed to get sync status response, " + e.message)
        }
        if (!exportTileList.isEmpty()) {
            exportTiles(exportTileList)

            // Reinitialize translations, as they may have been updated.
            database.setLanguage(config.languageCode)
            Log.d("ActiveCaptainManager", "Update complete, exports installed.")
        } else {
            Log.d("ActiveCaptainManager", "Update complete, no exports.")
        }
    }

    fun voteForReview(reviewId: Long) {
        if (jwt != null) {
            val call = apiInterface.voteForReview(reviewId, "Bearer $jwt")
            try {
                val response = call.execute()
                if (response.isSuccessful && response.body() != null) {
                    database.processVoteForReviewResponse(response.body()!!.string())
                }
            } catch (e: IOException) {
                Log.e("Error: ", "Failed to vote for review, " + e.message)
            }
        }
    }

    private fun exportTiles(tileList: Set<TileCoordinate>) {
        val tileRequests: MutableList<TileCoordinate> = ArrayList()
        for (tile in tileList) {
            tileRequests.add(TileCoordinate(tile.tileX, tile.tileY))
        }
        val call = apiInterface.getExports(tileRequests)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                exportDownloader.download(response.body())
            } else {
                Log.e(
                    "Error: ",
                    "Failed to get export URLs, " + response.code() + " " + response.message()
                )
            }
        } catch (e: IOException) {
            Log.e("Error: ", "Failed to get export URLs, " + e.message)
        }
    }

    private fun syncTileMarkers(tile: TileCoordinate): SyncResult {
        var result = SyncResult.FAIL
        var lastModifiedAfter: String? = ""
        var resultCount = 0
        do {
            val lastUpdateInfo = database.getTileLastModified(tile.tileX, tile.tileY)
            if (lastModifiedAfter == lastUpdateInfo.markerLastUpdate) {
                // Sanity check -- if lastModifiedAfter would be the same for multiple calls, break
                // out of the loop.  The API would return the same markers.
                break
            }
            lastModifiedAfter = lastUpdateInfo.markerLastUpdate
            val call = apiInterface.syncMarkers(tile.tileX, tile.tileY, lastModifiedAfter)
            try {
                val response = call.execute()
                if (response.isSuccessful && response.body() != null) {
                    try {
                        resultCount = database.processSyncMarkersResponse(
                            response.body()!!.string(),
                            tile.tileX,
                            tile.tileY
                        )
                        result = SyncResult.SUCCESS
                    } catch (e: IOException) {
                        Log.e(
                            "Error: ",
                            "Failed to read marker sync response, " + response.code() + " " + response.message()
                        )
                    }
                } else if (response.code() == 303) {
                    result = SyncResult.EXPORT_REQUIRED
                } else {
                    Log.e(
                        "Error: ",
                        "Failed to sync markers, " + response.code() + " " + response.message()
                    )
                    result = SyncResult.FAIL
                }
            } catch (e: IOException) {
                Log.e("Error: ", "Failed to read marker sync response, " + e.message)
                result = SyncResult.FAIL
            }
        } while (result == SyncResult.SUCCESS && resultCount == SYNC_MAX_RESULT_COUNT)
        return result
    }

    private fun syncTileReviews(tile: TileCoordinate): SyncResult {
        var result = SyncResult.FAIL
        var lastModifiedAfter: String? = ""
        var resultCount = 0
        do {
            val lastUpdateInfo = database.getTileLastModified(tile.tileX, tile.tileY)
            if (lastModifiedAfter == lastUpdateInfo.reviewLastUpdate) {
                // Sanity check -- if lastModifiedAfter would be the same for multiple calls, break
                // out of the loop.  The API would return the same markers.
                break
            }
            lastModifiedAfter = lastUpdateInfo.reviewLastUpdate
            val call = apiInterface.syncReviews(tile.tileX, tile.tileY, lastModifiedAfter)
            try {
                val response = call.execute()
                if (response.isSuccessful && response.body() != null) {
                    try {
                        resultCount = database.processSyncReviewsResponse(
                            response.body()!!.string(),
                            tile.tileX,
                            tile.tileY
                        )
                        result = SyncResult.SUCCESS
                    } catch (e: IOException) {
                        Log.e(
                            "Error: ",
                            "Failed to read review sync response, " + response.code() + " " + response.message()
                        )
                    }
                } else if (response.code() == 303) {
                    result = SyncResult.EXPORT_REQUIRED
                } else {
                    Log.e(
                        "Error: ",
                        "Failed to sync reviews, " + response.code() + " " + response.message()
                    )
                }
            } catch (e: IOException) {
                Log.e("Error: ", "Failed to read review sync response, " + e.message)
            }
        } while (result == SyncResult.SUCCESS && resultCount == SYNC_MAX_RESULT_COUNT)
        return result
    }

    companion object {
        private const val SYNC_MAX_RESULT_COUNT = 100
        private const val JWT_KEY = "JWT"
        private var basePath: String? = null
        private var sharedPreferences: SharedPreferences? = null
        private var config: ActiveCaptainConfiguration? = null

        // Must be called before calling GetInstance() the first time.
        @JvmStatic
        fun init(_basePath: String?, _sharedPreferences: SharedPreferences?, _config: ActiveCaptainConfiguration) {
            basePath = _basePath
            sharedPreferences = _sharedPreferences
            config = _config
        }

        @JvmStatic val instance by lazy {
            config?.let {
                ActiveCaptainManager(it)
            } ?: run {
                throw IllegalStateException("init must be run 1st")
            }
        }
    }
}