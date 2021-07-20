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

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import im.vector.app.R
import im.vector.app.core.preference.PushRulePreference.NotificationIndex
import im.vector.app.core.preference.PushRulePreference
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.toast
import im.vector.app.features.settings.VectorSettingsBaseFragment
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.pushrules.RuleIds
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.rest.PushRuleAndKind
import org.matrix.android.sdk.api.pushrules.toJson
import javax.inject.Inject

class VectorSettingsAdvancedNotificationPreferenceFragment @Inject constructor()
    : VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.settings_notification_advanced

    override val preferenceXmlRes = R.xml.vector_settings_notification_advanced_preferences

    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorPreference>(preferenceKey)
            if (preference is PushRulePreference) {
                // preference.isEnabled = null != rules && isConnected && pushManager.areDeviceNotificationsAllowed()
                val ruleAndKind: PushRuleAndKind? = session.getPushRules().findDefaultRule(prefKeyToPushRuleId[preferenceKey])

                if (ruleAndKind == null) {
                    // The rule is not defined, hide the preference
                    preference.isVisible = false
                } else {
                    preference.isVisible = true
                    val initialIndex = getNotificationIndexForRule(ruleAndKind.pushRule)
                    preference.setIndex(initialIndex)
                    preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        val newIndex = newValue as NotificationIndex
                        val standardAction = getStandardAction(ruleAndKind.pushRule.ruleId, newIndex)
                        if (standardAction != null) {
                            val enabled = standardAction != StandardActions.Disabled
                            val newActions = standardAction.actions
                            displayLoadingView()

                            lifecycleScope.launch {
                                val result = runCatching {
                                    session.updatePushRuleActions(ruleAndKind.kind,
                                            ruleAndKind.pushRule.ruleId,
                                            enabled,
                                            newActions)
                                }
                                if (!isAdded) {
                                    return@launch
                                }
                                hideLoadingView()
                                result.onSuccess {
                                    preference.setIndex(newIndex)
                                }
                                result.onFailure { failure ->
                                    // Restore the previous value
                                    refreshDisplay()
                                    activity?.toast(errorFormatter.toHumanReadable(failure))
                                }
                            }
                        }
                        false
                    }
                }
            }
        }
    }

    private fun getNotificationIndexForRule(rule: PushRule): NotificationIndex? {
        return NotificationIndex.values().firstOrNull {
            // Get the actions for the index
            val standardAction = getStandardAction(rule.ruleId, it) ?: return@firstOrNull false
            val indexActions = standardAction.actions ?: listOf()
            // Check if the input rule matches a rule generated from the static rule definitions
            val targetRule = rule.copy(enabled = standardAction != StandardActions.Disabled, actions = indexActions.toJson())
            ruleMatches(rule, targetRule)
        }
    }

    private fun ruleMatches(rule: PushRule, targetRule: PushRule): Boolean {
        // Rules match if both are disabled, or if both are enabled and their highlight/sound/notify actions match up.
        return (!rule.enabled && !targetRule.enabled)
                || (rule.enabled
                        && targetRule.enabled
                        && rule.getHighlight() == targetRule.getHighlight()
                        && rule.getNotificationSound() == targetRule.getNotificationSound()
                        && rule.shouldNotify() == targetRule.shouldNotify()
                        && rule.shouldNotNotify() == targetRule.shouldNotNotify())
    }

    private fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }

    /* ==========================================================================================
     * Companion
     * ========================================================================================== */

    companion object {
        //  preference name <-> rule Id
        private val prefKeyToPushRuleId = mapOf(
                "SETTINGS_PUSH_RULE_CONTAINING_MY_DISPLAY_NAME_PREFERENCE_KEY" to RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME,
                "SETTINGS_PUSH_RULE_CONTAINING_MY_USER_NAME_PREFERENCE_KEY" to RuleIds.RULE_ID_CONTAIN_USER_NAME,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_ONE_TO_ONE_PREFERENCE_KEY" to RuleIds.RULE_ID_ONE_TO_ONE_ROOM,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_GROUP_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS,
                "SETTINGS_PUSH_RULE_INVITED_TO_ROOM_PREFERENCE_KEY" to RuleIds.RULE_ID_INVITE_ME,
                "SETTINGS_PUSH_RULE_CALL_INVITATIONS_PREFERENCE_KEY" to RuleIds.RULE_ID_CALL,
                "SETTINGS_PUSH_RULE_MESSAGES_SENT_BY_BOT_PREFERENCE_KEY" to RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS,
                "SETTINGS_PUSH_RULE_MESSAGES_CONTAINING_AT_ROOM_PREFERENCE_KEY" to RuleIds.RULE_ID_ROOM_NOTIF,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_E2E_ONE_ONE_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ONE_TO_ONE_ENCRYPTED_ROOM,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_E2E_GROUP_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ENCRYPTED,
                "SETTINGS_PUSH_RULE_ROOMS_UPGRADED_KEY" to RuleIds.RULE_ID_TOMBSTONE
        )
    }
}
