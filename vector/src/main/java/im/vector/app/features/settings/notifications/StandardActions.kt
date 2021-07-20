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

package im.vector.app.features.settings.notifications

import org.matrix.android.sdk.api.pushrules.Action

sealed class StandardActions(
        val actions: List<Action>?
) {
    object Notify : StandardActions(actions = listOf(Action.Notify))
    object NotifyDefaultSound : StandardActions(actions = listOf(Action.Notify, Action.Sound()))
    object NotifyRingSound : StandardActions(actions = listOf(Action.Notify, Action.Sound(sound = Action.ACTION_OBJECT_VALUE_VALUE_RING)))
    object Highlight : StandardActions(actions = listOf(Action.Notify, Action.Highlight(highlight = true)))
    object HighlightDefaultSound : StandardActions(actions = listOf(Action.Notify, Action.Highlight(highlight = true), Action.Sound()))
    object DontNotify : StandardActions(actions = listOf(Action.DoNotNotify))
    object Disabled : StandardActions(actions = null)
}
