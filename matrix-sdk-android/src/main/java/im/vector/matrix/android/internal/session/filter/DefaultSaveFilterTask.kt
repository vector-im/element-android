/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.filter

import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

/**
 * Save a filter, in db and if any changes, upload to the server
 */
internal interface SaveFilterTask : Task<SaveFilterTask.Params, Unit> {

    data class Params(
            val filterPreset: FilterService.FilterPreset
    )
}

internal class DefaultSaveFilterTask @Inject constructor(
        @UserId private val userId: String,
        private val filterAPI: FilterApi,
        private val filterRepository: FilterRepository,
        private val eventBus: EventBus
) : SaveFilterTask {

    override suspend fun execute(params: SaveFilterTask.Params) {
        val filterBody = when (params.filterPreset) {
            FilterService.FilterPreset.RiotFilter -> {
                FilterFactory.createRiotFilterBody()
            }
            FilterService.FilterPreset.NoFilter   -> {
                FilterFactory.createDefaultFilterBody()
            }
        }
        val roomFilter = when (params.filterPreset) {
            FilterService.FilterPreset.RiotFilter -> {
                FilterFactory.createRiotRoomFilter()
            }
            FilterService.FilterPreset.NoFilter   -> {
                FilterFactory.createDefaultRoomFilter()
            }
        }
        val updated = filterRepository.storeFilter(filterBody, roomFilter)
        if (updated) {
            val filterResponse = executeRequest<FilterResponse>(eventBus) {
                // TODO auto retry
                apiCall = filterAPI.uploadFilter(userId, filterBody)
            }
            filterRepository.storeFilterId(filterBody, filterResponse.filterId)
        }
    }
}
