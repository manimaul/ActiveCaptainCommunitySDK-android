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
package com.garmin.marine.activecaptain.internal


data class ActiveCaptainConfiguration(
    val baseUrl: String,
    val apiKey: String,
    val ssoUrl: String,
    val markerMinSearchLength: Int = 3,
    val markerMaxSearchResults: Int = 100,
    val reviewListPageSize: Int = 10,
    val updateIntervalMinutes: Int = 15, // in minutes, must be >= 15
    val webViewBaseUrl: String,
    val webViewDebug: Boolean,
    val languageCode: String = "en_US",
) {
    companion object {
        @JvmStatic
        fun staging(apiKey: String) = ActiveCaptainConfiguration(
            baseUrl = "https://activecaptain-stage.garmin.com/community/thirdparty/",
            apiKey = apiKey,
            ssoUrl = "https://ssotest.garmin.com/sso/embed?clientId=ACTIVE_CAPTAIN_WEB&locale=en_US",
            webViewBaseUrl = "https://activecaptain-stage.garmin.com",
            webViewDebug = true,
        )

        @JvmStatic
        fun production(apiKey: String) = ActiveCaptainConfiguration(
            baseUrl = "https://activecaptain.garmin.com/community/thirdparty/",
            apiKey = apiKey,
            ssoUrl = "https://sso.garmin.com/sso/embed?clientId=ACTIVE_CAPTAIN_WEB&locale=en_US",
            webViewBaseUrl = "https://activecaptain.garmin.com",
            webViewDebug = false,
        )
    }
}
