package com.sgroupmobile.glowza.ui.profile

import android.content.res.Configuration
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    companion object {
        var currentTabId: Int = R.id.nav_home
    }

    override fun provideBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun setupUI() {
        lifecycleScope.launch {
            val savedLanguage = settingsDataStore.language.first()
            setupLanguage(savedLanguage)
        }

        setupDarkMode()
    }

    private fun setupLanguage(currentSavedLang: String) {
        val languages = resources.getStringArray(R.array.language_options)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = adapter

        val initialPosition = if (currentSavedLang == "en") 1 else 0
        binding.spinnerLanguage.setSelection(initialPosition, false)

        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCode = if (position == 1) "en" else "vi"

                    val currentAppLang = Locale.getDefault().language

                    if (selectedCode != currentAppLang) {
                        lifecycleScope.launch {
                            currentTabId = R.id.nav_profile
                            settingsDataStore.setLanguage(selectedCode)
                            changeLanguage(selectedCode)
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
    }

    private fun setupDarkMode() {
        // Kiểm tra xem giao diện HIỆN TẠI đang là Dark hay Light (bất kể là do hệ thống hay do app ép buộc)
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isCurrentlyDark = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        // Set trạng thái Switch cho đúng với giao diện hiện tại
        binding.switchDark.setOnCheckedChangeListener(null)
        binding.switchDark.isChecked = isCurrentlyDark

        // Xử lý sự kiện khi user tự tay bấm Switch
        binding.switchDark.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            lifecycleScope.launch {
                currentTabId = R.id.nav_profile

                // Lưu giá trị Int vào DataStore
                settingsDataStore.setDarkMode(mode)

                // Ép app đổi theme ngay lập tức
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    private fun changeLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        requireActivity().resources.updateConfiguration(config, resources.displayMetrics)
        requireActivity().recreate()
    }
}