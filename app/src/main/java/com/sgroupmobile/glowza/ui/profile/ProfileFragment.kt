package com.sgroupmobile.glowza.ui.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.data.data_store.setting.SettingsDataStore
import com.sgroupmobile.glowza.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun provideBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupLanguage()
        setupDarkMode()
    }

    private fun setupLanguage() {
        val languages = listOf("English", "Tiếng Việt")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCode = if (position == 0) "en" else "vi"
                val currentLang = resources.configuration.locales[0].language

                if (selectedCode != currentLang) {
                    lifecycleScope.launch {
                        // LƯU TAB VÀ NGÔN NGỮ TRƯỚC KHI RECREATE
                        settingsDataStore.setLastTab(R.id.nav_profile)
                        settingsDataStore.setLanguage(selectedCode)

                        changeLanguage(selectedCode)
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupDarkMode() {
        // Tắt listener để không bị loop khi gán giá trị khởi tạo
        binding.switchDark.setOnCheckedChangeListener(null)

        binding.switchDark.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        binding.switchDark.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            lifecycleScope.launch {
                settingsDataStore.setLastTab(R.id.nav_profile) // Lưu tab
                settingsDataStore.setDarkMode(mode)
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    private fun changeLanguage(languageCode: String) {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        requireActivity().recreate()
    }
}