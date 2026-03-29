package com.sgroupmobile.glowza.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.data.data_store.setting.SettingsDataStore
import com.sgroupmobile.glowza.data.data_store.setting.settingsDataStore
import com.sgroupmobile.glowza.databinding.ActivityMainBinding
import com.sgroupmobile.glowza.extension.dpToPx
import com.sgroupmobile.glowza.ui.home.fragment.HomeFragment
import com.sgroupmobile.glowza.ui.onboarding.OnboardingActivity
import com.sgroupmobile.glowza.ui.profile.ProfileFragment
import com.sgroupmobile.glowza.util.saveImageToGallery
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun provideBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        var isLoading = true

        splashScreen.setKeepOnScreenCondition { isLoading }

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val isFirstRun = settingsDataStore.isFirstRun.first()
            isLoading = false

            if (isFirstRun) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
            }

            val mode = settingsDataStore.mode.first()
            if (mode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = kotlinx.coroutines.runBlocking {
            val preferences = newBase.settingsDataStore.data.first()
            preferences[stringPreferencesKey("app_language")] ?: "vi"
        }

        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)

        super.attachBaseContext(context)
    }

    override fun setupUI() {
        setupCustomBottomNav()

        // set default tab
        if (ProfileFragment.currentTabId == R.id.nav_profile) {
            selectTab(R.id.nav_profile)
            replaceFragment(ProfileFragment())
        } else {
            selectTab(R.id.nav_home)
            replaceFragment(HomeFragment())
        }
        setupInset(binding.root, binding.bottomBar)
    }
    private fun setupCustomBottomNav() {
        binding.navHome.setOnClickListener {
            selectTab(R.id.nav_home)
            replaceFragment(HomeFragment())
        }

        binding.navProfile.setOnClickListener {
            selectTab(R.id.nav_profile)
            replaceFragment(ProfileFragment())
        }
    }
    private fun selectTab(tabId: Int) {
        val activeColor = getColor(R.color.onBackground)
        val inactiveColor = getColor(R.color.surfaceVariant)

        // reset
        binding.iconHome.setColorFilter(inactiveColor)
        binding.textHome.setTextColor(inactiveColor)

        binding.iconProfile.setColorFilter(inactiveColor)
        binding.textProfile.setTextColor(inactiveColor)

        // active
        when (tabId) {
            R.id.nav_home -> {
                binding.iconHome.setColorFilter(activeColor)
                binding.textHome.setTextColor(activeColor)
            }
            R.id.nav_profile -> {
                binding.iconProfile.setColorFilter(activeColor)
                binding.textProfile.setTextColor(activeColor)
            }
        }
    }

    override fun setupInset(topView: View, bottomView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(topView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = bottomView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(
                systemBars.left,
                0,
                systemBars.right,
                systemBars.bottom + 14.dpToPx()
            )
            bottomView.layoutParams = params
            insets
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment?.javaClass == fragment.javaClass) return

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    fun updateStatusBarColor(isLight: Boolean = false){
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = isLight
    }
}