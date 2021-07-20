/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.rest.RuleSet
import org.matrix.android.sdk.api.session.events.model.Event

interface PushRuleService {
    /**
     * Fetch the push rules from the server
     */
    fun fetchPushRules(scope: String = RuleScope.GLOBAL)

    fun getPushRules(scope: String = RuleScope.GLOBAL): RuleSet

    suspend fun updatePushRuleEnableStatus(kind: RuleKind, pushRule: PushRule, enabled: Boolean)

    suspend fun addPushRule(kind: RuleKind, pushRule: PushRule)

    /**
     * Enables/Disables a push rule and updates the actions if necessary
     * @param enable Enables/Disables the rule
     * @param actions Actions to update if not null
     */

    suspend fun updatePushRuleActions(kind: RuleKind, ruleId: String, enable: Boolean, actions: List<Action>?)

    suspend fun removePushRule(kind: RuleKind, pushRule: PushRule)

    fun addPushRuleListener(listener: PushRuleListener)

    fun removePushRuleListener(listener: PushRuleListener)

    fun getActions(event: Event): List<Action>

//    fun fulfilledBingRule(event: Event, rules: List<PushRule>): PushRule?

    interface PushRuleListener {
        fun onMatchRule(event: Event, actions: List<Action>)
        fun onRoomJoined(roomId: String)
        fun onRoomLeft(roomId: String)
        fun onEventRedacted(redactedEventId: String)
        fun batchFinish()
    }
}
