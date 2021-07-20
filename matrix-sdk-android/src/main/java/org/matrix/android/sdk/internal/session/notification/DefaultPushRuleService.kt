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
package org.matrix.android.sdk.internal.session.notification

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.pushrules.PushRuleService
import org.matrix.android.sdk.api.pushrules.RuleKind
import org.matrix.android.sdk.api.pushrules.RuleScope
import org.matrix.android.sdk.api.pushrules.RuleSetKey
import org.matrix.android.sdk.api.pushrules.getActions
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.rest.RuleSet
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.database.mapper.PushRulesMapper
import org.matrix.android.sdk.internal.database.model.PushRulesEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.pushers.AddPushRuleTask
import org.matrix.android.sdk.internal.session.pushers.GetPushRulesTask
import org.matrix.android.sdk.internal.session.pushers.RemovePushRuleTask
import org.matrix.android.sdk.internal.session.pushers.UpdatePushRuleActionsTask
import org.matrix.android.sdk.internal.session.pushers.UpdatePushRuleEnableStatusTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultPushRuleService @Inject constructor(
        private val getPushRulesTask: GetPushRulesTask,
        private val updatePushRuleEnableStatusTask: UpdatePushRuleEnableStatusTask,
        private val addPushRuleTask: AddPushRuleTask,
        private val updatePushRuleActionsTask: UpdatePushRuleActionsTask,
        private val removePushRuleTask: RemovePushRuleTask,
        private val pushRuleFinder: PushRuleFinder,
        private val taskExecutor: TaskExecutor,
        @SessionDatabase private val monarchy: Monarchy
) : PushRuleService {

    private var listeners = mutableSetOf<PushRuleService.PushRuleListener>()

    override fun fetchPushRules(scope: String) {
        getPushRulesTask
                .configureWith(GetPushRulesTask.Params(scope))
                .executeBy(taskExecutor)
    }

    override fun getPushRules(scope: String): RuleSet {
        var contentRules: List<PushRule> = emptyList()
        var overrideRules: List<PushRule> = emptyList()
        var roomRules: List<PushRule> = emptyList()
        var senderRules: List<PushRule> = emptyList()
        var underrideRules: List<PushRule> = emptyList()

        monarchy.doWithRealm { realm ->
            PushRulesEntity.where(realm, scope, RuleSetKey.CONTENT)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        contentRules = pushRulesEntity.pushRules.map { PushRulesMapper.mapContentRule(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.OVERRIDE)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        overrideRules = pushRulesEntity.pushRules.map { PushRulesMapper.map(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.ROOM)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        roomRules = pushRulesEntity.pushRules.map { PushRulesMapper.mapRoomRule(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.SENDER)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        senderRules = pushRulesEntity.pushRules.map { PushRulesMapper.mapSenderRule(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.UNDERRIDE)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        underrideRules = pushRulesEntity.pushRules.map { PushRulesMapper.map(it) }
                    }
        }

        return RuleSet(
                content = contentRules,
                override = overrideRules,
                room = roomRules,
                sender = senderRules,
                underride = underrideRules
        )
    }

    override suspend fun updatePushRuleEnableStatus(kind: RuleKind, pushRule: PushRule, enabled: Boolean) {
        // The rules will be updated, and will come back from the next sync response
        updatePushRuleEnableStatusTask.execute(UpdatePushRuleEnableStatusTask.Params(kind, pushRule, enabled))
    }

    override suspend fun addPushRule(kind: RuleKind, pushRule: PushRule) {
        addPushRuleTask.execute(AddPushRuleTask.Params(kind, pushRule))
    }

    override suspend fun updatePushRuleActions(kind: RuleKind, ruleId: String, enable: Boolean, actions: List<Action>?) {
        updatePushRuleActionsTask.execute(UpdatePushRuleActionsTask.Params(kind, ruleId, enable, actions))
    }

    override suspend fun removePushRule(kind: RuleKind, pushRule: PushRule) {
        removePushRuleTask.execute(RemovePushRuleTask.Params(kind, pushRule))
    }

    override fun removePushRuleListener(listener: PushRuleService.PushRuleListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun addPushRuleListener(listener: PushRuleService.PushRuleListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    override fun getActions(event: Event): List<Action> {
        val rules = getPushRules(RuleScope.GLOBAL).getAllRules()

        return pushRuleFinder.fulfilledBingRule(event, rules)?.getActions().orEmpty()
    }

//    fun processEvents(events: List<Event>) {
//        var hasDoneSomething = false
//        events.forEach { event ->
//            fulfilledBingRule(event)?.let {
//                hasDoneSomething = true
//                dispatchBing(event, it)
//            }
//        }
//        if (hasDoneSomething)
//            dispatchFinish()
//    }

    fun dispatchBing(event: Event, rule: PushRule) {
        synchronized(listeners) {
            val actionsList = rule.getActions()
            listeners.forEach {
                try {
                    it.onMatchRule(event, actionsList)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching bing")
                }
            }
        }
    }

    fun dispatchRoomJoined(roomId: String) {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.onRoomJoined(roomId)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching room joined")
                }
            }
        }
    }

    fun dispatchRoomLeft(roomId: String) {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.onRoomLeft(roomId)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching room left")
                }
            }
        }
    }

    fun dispatchRedactedEventId(redactedEventId: String) {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.onEventRedacted(redactedEventId)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching redacted event")
                }
            }
        }
    }

    fun dispatchFinish() {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.batchFinish()
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching finish")
                }
            }
        }
    }
}
