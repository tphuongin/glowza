package com.sgroupmobile.glowza.ui.onboarding

import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseActivity
import com.sgroupmobile.glowza.data.data_store.setting.SettingsDataStore
import com.sgroupmobile.glowza.data.model.OnboardingItem
import com.sgroupmobile.glowza.databinding.ActivityOnBoardingBinding
import com.sgroupmobile.glowza.ui.home.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnBoardingBinding>() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun provideBinding() = ActivityOnBoardingBinding.inflate(layoutInflater)

    override fun setupUI() {
        val items = listOf(
            OnboardingItem(R.string.ob_title_1, R.string.ob_desc_1, R.drawable.onboarding1),
            OnboardingItem(R.string.ob_title_2, R.string.ob_desc_2, R.drawable.onboarding2),
            OnboardingItem(R.string.ob_title_3, R.string.ob_desc_3, R.drawable.onboarding3)
        )

        binding.viewPager.adapter = OnboardingAdapter(items)
        setupIndicators(items.size)
        setCurrentIndicator(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setCurrentIndicator(position)
                if (position == items.size - 1) {
                    binding.btnNext.text = getString(R.string.start_now)
                } else {
                    binding.btnNext.text = getString(R.string.next)
                }
            }
        })

        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem + 1 < items.size) {
                binding.viewPager.currentItem += 1
            } else {
                navigateToMain()
            }
        }
    }

    override fun setupListeners() {
        binding.btnSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun setupIndicators(size: Int) {
        val indicators = arrayOfNulls<ImageView>(size)
        val layoutParams = LinearLayout.LayoutParams(20, 20).apply { setMargins(8, 0, 8, 0) }
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.indicator_inactive))
                this.layoutParams = layoutParams
            }
            binding.layoutIndicators.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = binding.layoutIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = binding.layoutIndicators.getChildAt(i) as ImageView
            val layoutParams = imageView.layoutParams as LinearLayout.LayoutParams

            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_active))
                layoutParams.width = 45 // Dấu chấm đang chọn sẽ dài ra
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive))
                layoutParams.width = 20
            }
            imageView.layoutParams = layoutParams
        }
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            settingsDataStore.setFirstRunComplete() // Lưu là đã xem
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }

}