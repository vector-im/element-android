/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RadioGroup
import androidx.preference.PreferenceViewHolder
import im.vector.app.R

class PushRulePreference : VectorPreference {

    enum class NotificationIndex(val index: Int) {
        OFF(0),
        SILENT(1),
        NOISY(2);

        companion object {
            fun fromInt(index: Int) = values().first { it.index == index }
        }
    }

    /**
     * @return the selected push rule index
     */
    var index: NotificationIndex? = null
        private set

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        layoutResource = R.layout.vector_preference_push_rule
    }

    /**
     * Update the notification index.
     *
     * @param pushRule
     */
    fun setIndex(notificationIndex: NotificationIndex?) {
        index = notificationIndex
        refreshSummary()
    }

    /**
     * Refresh the summary
     */
    private fun refreshSummary() {
        summary = context.getString(when (index) {
            NotificationIndex.OFF           -> R.string.notification_off
            NotificationIndex.SILENT      -> R.string.notification_silent
            NotificationIndex.NOISY, null -> R.string.notification_noisy
        })
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(android.R.id.summary)?.visibility = View.GONE
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        val radioGroup = holder.findViewById(R.id.bingPreferenceRadioGroup) as? RadioGroup
        radioGroup?.setOnCheckedChangeListener(null)

        when (index) {
            NotificationIndex.OFF       -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleOff)
            }
            NotificationIndex.SILENT -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleSilent)
            }
            NotificationIndex.NOISY  -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleNoisy)
            }
        }

        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.bingPreferenceRadioBingRuleOff    -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NotificationIndex.OFF)
                }
                R.id.bingPreferenceRadioBingRuleSilent -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NotificationIndex.SILENT)
                }
                R.id.bingPreferenceRadioBingRuleNoisy  -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NotificationIndex.NOISY)
                }
            }
        }
    }
}
