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

package org.matrix.android.sdk.api.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.pushrules.getActions
import org.matrix.android.sdk.api.pushrules.toJson

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
@JsonClass(generateAdapter = true)
data class PushRule(
        /**
         * Required. The actions to perform when this rule is matched.
         */
        @Json(name = "actions")
        val actions: List<Any>,
        /**
         * Required. Whether this is a default rule, or has been set explicitly.
         */
        @Json(name = "default")
        val default: Boolean? = false,
        /**
         * Required. Whether the push rule is enabled or not.
         */
        @Json(name = "enabled")
        val enabled: Boolean,
        /**
         * Required. The ID of this rule.
         */
        @Json(name = "rule_id")
        val ruleId: String,
        /**
         * The conditions that must hold true for an event in order for a rule to be applied to an event
         */
        @Json(name = "conditions")
        val conditions: List<PushCondition>? = null,
        /**
         * The glob-style pattern to match against. Only applicable to content rules.
         */
        @Json(name = "pattern")
        val pattern: String? = null
) {
    /**
     * Add the default notification sound.
     */
    fun setNotificationSound(): PushRule {
        return setNotificationSound(Action.ACTION_OBJECT_VALUE_VALUE_DEFAULT)
    }

    fun getNotificationSound(): String? {
        return (getActions().firstOrNull { it is Action.Sound } as? Action.Sound)?.sound
    }

    /**
     * Set the notification sound
     *
     * @param sound notification sound
     */
    fun setNotificationSound(sound: String): PushRule {
        return copy(
                actions = (getActions().filter { it !is Action.Sound } + Action.Sound(sound)).toJson()
        )
    }

    /**
     * Remove the notification sound
     */
    fun removeNotificationSound(): PushRule {
        return copy(
                actions = getActions().filter { it !is Action.Sound }.toJson()
        )
    }

    /**
     * Set the highlight status.
     *
     * @param highlight the highlight status
     */
    fun setHighlight(highlight: Boolean): PushRule {
        return copy(
                actions = (getActions().filter { it !is Action.Highlight } + Action.Highlight(highlight)).toJson()
        )
    }

    /**
     * Get the highlight status. As spec mentions assume false if no tweak present.
     */
    fun getHighlight(): Boolean {
        return (getActions().firstOrNull { it is Action.Highlight } as? Action.Highlight)?.highlight.orFalse()
    }

    /**
     * Set the notification status.
     *
     * @param notify true to notify
     */
    fun setNotify(notify: Boolean): PushRule {
        val mutableActions = actions.toMutableList()

        mutableActions.remove(Action.ACTION_DONT_NOTIFY)
        mutableActions.remove(Action.ACTION_NOTIFY)

        if (notify) {
            mutableActions.add(Action.ACTION_NOTIFY)
        } else {
            mutableActions.add(Action.ACTION_DONT_NOTIFY)
        }

        return copy(actions = mutableActions)
    }

    /**
     * Return true if the rule should highlight the event.
     *
     * @return true if the rule should play sound
     */
    fun shouldNotify() = actions.contains(Action.ACTION_NOTIFY)

    /**
     * Return true if the rule should not highlight the event.
     *
     * @return true if the rule should not play sound
     */
    fun shouldNotNotify() = actions.contains(Action.ACTION_DONT_NOTIFY)
}
