package com.sgroupmobile.glowza.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.data.data_store.setting.SettingsDataStore
import com.sgroupmobile.glowza.data.data_store.setting.settingsDataStore
import com.sgroupmobile.glowza.databinding.ActivityMainBinding
import com.sgroupmobile.glowza.ui.home.fragment.HomeFragment
import com.sgroupmobile.glowza.ui.onboarding.OnboardingActivity
import com.sgroupmobile.glowza.ui.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
            val isDarkModeSet = settingsDataStore.isDarkMode.first()

            // Nếu lần đầu, dùng chế độ hệ thống
            val mode = if (isDarkModeSet) {
                settingsDataStore.mode.first()
            } else {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            AppCompatDelegate.setDefaultNightMode(mode)
            isLoading = false

            if (isFirstRun) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = runBlocking {
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
        setupBottomNav()
        binding.bottomNav.selectedItemId = ProfileFragment.currentTabId
    }

    override fun setupInset(topView: View, bottomView: View) {}

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            fragment?.let {
                replaceFragment(it)
                true
            } ?: false
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