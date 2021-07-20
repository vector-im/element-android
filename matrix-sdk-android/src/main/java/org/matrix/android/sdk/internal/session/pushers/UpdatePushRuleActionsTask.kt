/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.pushrules.RuleKind
import org.matrix.android.sdk.api.pushrules.toJson
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdatePushRuleActionsTask : Task<UpdatePushRuleActionsTask.Params, Unit> {
    data class Params(
            val kind: RuleKind,
            val ruleId: String,
            val enable: Boolean,
            val actions: List<Action>?
    )
}

internal class DefaultUpdatePushRuleActionsTask @Inject constructor(
        private val pushRulesApi: PushRulesApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdatePushRuleActionsTask {

    override suspend fun execute(params: UpdatePushRuleActionsTask.Params) {
            executeRequest(globalErrorReceiver) {
                pushRulesApi.updateEnableRuleStatus(params.kind.value, params.ruleId, enable = params.enable)
            }
            if (params.actions != null) {
                val body = mapOf("actions" to params.actions.toJson())
                executeRequest(globalErrorReceiver) {
                    pushRulesApi.updateRuleActions(params.kind.value, params.ruleId, body)
                }
            }
    }
}
