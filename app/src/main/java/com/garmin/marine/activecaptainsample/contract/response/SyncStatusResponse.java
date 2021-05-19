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

package com.garmin.marine.activecaptainsample.contract.response;

import com.google.gson.annotations.SerializedName;

public class SyncStatusResponse {
    public enum SyncStatusType {
        Export,
        Sync,
        Delete,
        None
    }

    @SerializedName("tileX")
    public int TileX;

    @SerializedName("tileY")
    public int TileY;

    @SerializedName("poiUpdateType")
    public SyncStatusType PoiUpdateType;

    @SerializedName("reviewUpdateType")
    public SyncStatusType ReviewUpdateType;
}
