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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSettingsNotificationsTroubleshootBinding
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.settings.VectorSettingsFragmentInteractionListener
import im.vector.app.features.settings.troubleshoot.NotificationTroubleshootTestManager
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.app.push.fcm.NotificationTroubleshootTestManagerFactory

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject

class VectorSettingsNotificationsTroubleshootFragment @Inject constructor(
        private val bugReporter: BugReporter,
        private val testManagerFactory: NotificationTroubleshootTestManagerFactory
) : VectorBaseFragment<FragmentSettingsNotificationsTroubleshootBinding>() {

    private var testManager: NotificationTroubleshootTestManager? = null
    // members

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsNotificationsTroubleshootBinding {
        return FragmentSettingsNotificationsTroubleshootBinding.inflate(inflater, container, false)
    }

    private var interactionListener: VectorSettingsFragmentInteractionListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        views.troubleshootTestRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(view.context, layoutManager.orientation)
        views.troubleshootTestRecyclerView.addItemDecoration(dividerItemDecoration)

        views.troubleshootSummButton.debouncedClicks {
            bugReporter.openBugReportScreen(requireActivity())
        }

        views.troubleshootRunButton.debouncedClicks {
            testManager?.retry(testStartForActivityResult)
        }
        startUI()
    }

    private fun startUI() {
        views.toubleshootSummDescription.text = getString(R.string.settings_troubleshoot_diagnostic_running_status, 0, 0)
        testManager = testManagerFactory.create(this)
        testManager?.statusListener = { troubleshootTestManager ->
            if (isAdded) {
                TransitionManager.beginDelayedTransition(views.troubleshootBottomView)
                when (troubleshootTestManager.diagStatus) {
                    TroubleshootTest.TestStatus.NOT_STARTED      -> {
                        views.toubleshootSummDescription.text = ""
                        views.troubleshootSummButton.visibility = View.GONE
                        views.troubleshootRunButton.visibility = View.VISIBLE
                    }
                    TroubleshootTest.TestStatus.RUNNING,
                    TroubleshootTest.TestStatus.WAITING_FOR_USER -> {
                        val size = troubleshootTestManager.testListSize
                        val currentTestIndex = troubleshootTestManager.currentTestIndex
                        views.toubleshootSummDescription.text = getString(
                                R.string.settings_troubleshoot_diagnostic_running_status,
                                currentTestIndex,
                                size
                        )
                        views.troubleshootSummButton.visibility = View.GONE
                        views.troubleshootRunButton.visibility = View.GONE
                    }
                    TroubleshootTest.TestStatus.FAILED           -> {
                        // check if there are quick fixes
                        val hasQuickFix = testManager?.hasQuickFix().orFalse()
                        if (hasQuickFix) {
                            views.toubleshootSummDescription.text = getString(R.string.settings_troubleshoot_diagnostic_failure_status_with_quickfix)
                        } else {
                            views.toubleshootSummDescription.text = getString(R.string.settings_troubleshoot_diagnostic_failure_status_no_quickfix)
                        }
                        views.troubleshootSummButton.visibility = View.VISIBLE
                        views.troubleshootRunButton.visibility = View.VISIBLE
                    }
                    TroubleshootTest.TestStatus.SUCCESS          -> {
                        views.toubleshootSummDescription.text = getString(R.string.settings_troubleshoot_diagnostic_success_status)
                        views.troubleshootSummButton.visibility = View.VISIBLE
                        views.troubleshootRunButton.visibility = View.VISIBLE
                    }
                }
            }
        }
        views.troubleshootTestRecyclerView.adapter = testManager?.adapter
        testManager?.runDiagnostic(testStartForActivityResult)
    }

    override fun onDestroyView() {
        views.troubleshootTestRecyclerView.cleanup()
        super.onDestroyView()
    }

    private val testStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            retry()
        }
    }

    private fun retry() {
        testManager?.retry(testStartForActivityResult)
    }

    override fun onDetach() {
        testManager?.cancel()
        interactionListener = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.settings_notification_troubleshoot)

        tryOrNull("Unable to register the receiver") {
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(broadcastReceiverPush, IntentFilter(NotificationUtils.PUSH_ACTION))
        }
        tryOrNull("Unable to register the receiver") {
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(broadcastReceiverNotification, IntentFilter(NotificationUtils.DIAGNOSTIC_ACTION))
        }
    }

    override fun onPause() {
        super.onPause()
        tryOrNull {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(broadcastReceiverPush)
        }
        tryOrNull {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(broadcastReceiverNotification)
        }
    }

    private val broadcastReceiverPush = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testManager?.onDiagnosticPushReceived()
        }
    }

    private val broadcastReceiverNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testManager?.onDiagnosticNotificationClicked()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VectorSettingsFragmentInteractionListener) {
            interactionListener = context
        }
    }
}
