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
package im.vector.app.features.settings

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityVectorSettingsBinding
import im.vector.app.features.settings.devices.VectorSettingsDevicesFragment
import im.vector.app.features.settings.notifications.VectorSettingsNotificationPreferenceFragment

import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

/**
 * Displays the client settings.
 */
class VectorSettingsActivity : VectorBaseActivity<ActivityVectorSettingsBinding>(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        FragmentManager.OnBackStackChangedListener,
        VectorSettingsFragmentInteractionListener {

    override fun getBinding() = ActivityVectorSettingsBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getTitleRes() = R.string.title_activity_settings

    private var keyToHighlight: String? = null

    var ignoreInvalidTokenError = false

    @Inject lateinit var session: Session

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        configureToolbar(views.settingsToolbar)

        if (isFirstCreation()) {
            // display the fragment
            when (intent.getIntExtra(EXTRA_DIRECT_ACCESS, EXTRA_DIRECT_ACCESS_ROOT)) {
                EXTRA_DIRECT_ACCESS_GENERAL                          ->
                    replaceFragment(R.id.vector_settings_page, VectorSettingsGeneralFragment::class.java, null, FRAGMENT_TAG)
                EXTRA_DIRECT_ACCESS_ADVANCED_SETTINGS                ->
                    replaceFragment(R.id.vector_settings_page, VectorSettingsAdvancedSettingsFragment::class.java, null, FRAGMENT_TAG)
                EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY                 ->
                    replaceFragment(R.id.vector_settings_page, VectorSettingsSecurityPrivacyFragment::class.java, null, FRAGMENT_TAG)
                EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS ->
                    replaceFragment(R.id.vector_settings_page,
                            VectorSettingsDevicesFragment::class.java,
                            null,
                            FRAGMENT_TAG)
                EXTRA_DIRECT_ACCESS_NOTIFICATIONS                    -> {
                    requestHighlightPreferenceKeyOnResume(VectorPreferences.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY)
                    replaceFragment(R.id.vector_settings_page, VectorSettingsNotificationPreferenceFragment::class.java, null, FRAGMENT_TAG)
                }

                else                                                 ->
                    replaceFragment(R.id.vector_settings_page, VectorSettingsRootFragment::class.java, null, FRAGMENT_TAG)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()
    }

    override fun onBackStackChanged() {
        if (0 == supportFragmentManager.backStackEntryCount) {
            supportActionBar?.title = getString(getTitleRes())
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val oFragment = try {
            pref.fragment?.let {
                supportFragmentManager.fragmentFactory.instantiate(classLoader, it)
            }
        } catch (e: Throwable) {
            showSnackbar(getString(R.string.not_implemented))
            Timber.e(e)
            null
        }

        if (oFragment != null) {
            // Deprecated, I comment it, I think it is useless
            // oFragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.right_in, R.anim.fade_out, R.anim.fade_in, R.anim.right_out)
                    .replace(R.id.vector_settings_page, oFragment, pref.title.toString())
                    .addToBackStack(null)
                    .commit()
            return true
        }
        return false
    }

    override fun requestHighlightPreferenceKeyOnResume(key: String?) {
        keyToHighlight = key
    }

    override fun requestedKeyToHighlight(): String? {
        return keyToHighlight
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        if (ignoreInvalidTokenError) {
            Timber.w("Ignoring invalid token global error")
        } else {
            super.handleInvalidToken(globalError)
        }
    }

    fun <T : Fragment> navigateTo(fragmentClass: Class<T>) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.fade_out, R.anim.fade_in, R.anim.right_out)
                .replace(R.id.vector_settings_page, fragmentClass, null)
                .addToBackStack(null)
                .commit()
    }

    companion object {
        fun getIntent(context: Context, directAccess: Int) = Intent(context, VectorSettingsActivity::class.java)
                .apply { putExtra(EXTRA_DIRECT_ACCESS, directAccess) }

        private const val EXTRA_DIRECT_ACCESS = "EXTRA_DIRECT_ACCESS"

        const val EXTRA_DIRECT_ACCESS_ROOT = 0
        const val EXTRA_DIRECT_ACCESS_ADVANCED_SETTINGS = 1
        const val EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY = 2
        const val EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY_MANAGE_SESSIONS = 3
        const val EXTRA_DIRECT_ACCESS_GENERAL = 4
        const val EXTRA_DIRECT_ACCESS_NOTIFICATIONS = 5

        private const val FRAGMENT_TAG = "VectorSettingsPreferencesFragment"
    }
}
