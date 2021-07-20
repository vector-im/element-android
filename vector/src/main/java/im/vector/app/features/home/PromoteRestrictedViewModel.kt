/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

data class ActiveSpaceViewState(
        val isInSpaceMode: Boolean = false,
        val activeSpaceSummary: RoomSummary? = null,
        val canUserManageSpace: Boolean = false
) : MvRxState

class PromoteRestrictedViewModel @AssistedInject constructor(
        @Assisted initialState: ActiveSpaceViewState,
        private val activeSessionHolder: ActiveSessionHolder,
        private val appStateHandler: AppStateHandler
) : VectorViewModel<ActiveSpaceViewState, EmptyAction, EmptyViewEvents>(initialState) {

    init {
        appStateHandler.selectedRoomGroupingObservable.distinctUntilChanged().execute {
            val groupingMethod = it.invoke()?.orNull()
            val isSpaceMode = groupingMethod is RoomGroupingMethod.BySpace
            val currentSpace = (groupingMethod as? RoomGroupingMethod.BySpace)?.spaceSummary
            val canManage = currentSpace?.roomId?.let { roomId ->
                activeSessionHolder.getSafeActiveSession()
                        ?.getRoom(currentSpace.roomId)
                        ?.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.NoCondition)
                        ?.content?.toModel<PowerLevelsContent>()?.let {
                            PowerLevelsHelper(it).isUserAllowedToSend(activeSessionHolder.getActiveSession().myUserId, true, EventType.STATE_SPACE_CHILD)
                        } ?: false
            } ?: false

            copy(
                    isInSpaceMode = isSpaceMode,
                    activeSpaceSummary = currentSpace,
                    canUserManageSpace = canManage
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ActiveSpaceViewState): PromoteRestrictedViewModel
    }

    companion object : MvRxViewModelFactory<PromoteRestrictedViewModel, ActiveSpaceViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ActiveSpaceViewState): PromoteRestrictedViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: EmptyAction) {}
}
