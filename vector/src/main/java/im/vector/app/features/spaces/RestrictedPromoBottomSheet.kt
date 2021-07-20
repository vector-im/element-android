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

package im.vector.app.features.spaces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetSpaceAdvertiseRestrictedBinding

class RestrictedPromoBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSpaceAdvertiseRestrictedBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            BottomSheetSpaceAdvertiseRestrictedBinding.inflate(inflater, container, false)

    override val showExpanded = true

    var learnMoreMode: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        render()
        views.skipButton.debouncedClicks {
            dismiss()
        }

        views.learnMore.debouncedClicks {
            if (learnMoreMode) {
                dismiss()
            } else {
                learnMoreMode = true
                render()
            }
        }
    }

    private fun render() {
        if (learnMoreMode) {
            views.title.text = getString(R.string.new_let_people_in_spaces_find_and_join)
            views.topDescription.text = getString(R.string.to_help_space_members_find_and_join)
            views.imageHint.isVisible = true
            views.bottomDescription.isVisible = true
            views.bottomDescription.text = getString(R.string.this_makes_it_easy_for_rooms_to_stay_private_to_a_space)
            views.skipButton.isVisible = false
            views.learnMore.text = getString(R.string.ok)
        } else {
            views.title.text = getString(R.string.help_space_members)
            views.topDescription.text = getString(R.string.help_people_in_spaces_find_and_join)
            views.imageHint.isVisible = false
            views.bottomDescription.isVisible = false
            views.skipButton.isVisible = true
            views.learnMore.isVisible = true
            views.learnMore.text = getString(R.string.learn_more)
        }
    }
}
