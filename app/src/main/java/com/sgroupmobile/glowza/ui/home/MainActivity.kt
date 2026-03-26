package com.sgroupmobile.glowza.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.data.data_store.setting.SettingsDataStore
import com.sgroupmobile.glowza.databinding.ActivityMainBinding
import com.sgroupmobile.glowza.ui.home.fragment.HomeFragment
import com.sgroupmobile.glowza.ui.onboarding.OnboardingActivity
import com.sgroupmobile.glowza.ui.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun provideBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val isFirstRun = settingsDataStore.isFirstRun.first()
            if (isFirstRun) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
            }
        }
    }


    override fun setupUI() {
        setupBottomNav()
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

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
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment?.javaClass == fragment.javaClass) return

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}