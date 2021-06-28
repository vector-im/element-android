/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.version

import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.homeserver.RoomVersionStatus
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.version.RoomVersionService
import org.matrix.android.sdk.internal.database.mapper.HomeServerCapabilitiesMapper
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource

internal class DefaultRoomVersionService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val stateEventDataSource: StateEventDataSource,
        private val roomVersionUpgradeTask: RoomVersionUpgradeTask
) : RoomVersionService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultRoomVersionService
    }

    override fun getRoomVersion(): String {
        return stateEventDataSource.getStateEvent(roomId, EventType.STATE_ROOM_CREATE, QueryStringValue.IsEmpty)
                ?.content
                ?.toModel<RoomCreateContent>()
                ?.roomVersion
        // as per spec -> Defaults to "1" if the key does not exist.
                ?: DEFAULT_ROOM_VERSION
    }

    override suspend fun upgradeToVersion(version: String): String {
        return roomVersionUpgradeTask.execute(
                RoomVersionUpgradeTask.Params(
                        roomId, version
                )
        )
    }

    override fun getRecommendedVersion(): String {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            HomeServerCapabilitiesEntity.get(realm)?.let {
                HomeServerCapabilitiesMapper.map(it)
            }?.roomVersions?.defaultRoomVersion ?: DEFAULT_ROOM_VERSION
        }
    }

    override fun isUsingUnstableRoomVersion(): Boolean {
        var isUsingUnstable: Boolean
        Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            val versionCaps = HomeServerCapabilitiesEntity.get(realm)?.let {
                HomeServerCapabilitiesMapper.map(it)
            }?.roomVersions
            val currentVersion = getRoomVersion()
            isUsingUnstable = versionCaps?.supportedVersion?.firstOrNull { it.version == currentVersion }?.status == RoomVersionStatus.UNSTABLE
        }
        return isUsingUnstable
    }

    override fun userMayUpgradeRoom(userId: String): Boolean {
        val powerLevelsHelper = stateEventDataSource.getStateEvent(roomId, EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.NoCondition)
                ?.content?.toModel<PowerLevelsContent>()
                ?.let { PowerLevelsHelper(it) }

        return powerLevelsHelper?.isUserAllowedToSend(userId, true, EventType.STATE_ROOM_TOMBSTONE) ?: false
    }

    companion object {
        const val DEFAULT_ROOM_VERSION = "1"
    }
}