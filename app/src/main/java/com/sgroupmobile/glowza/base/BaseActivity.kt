package com.sgroupmobile.glowza.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding?: throw IllegalStateException("Binding is not initialized")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = provideBinding()
        setContentView(binding.root)
        setupInset()
        setupUI()
        initData()
        setupObservers()
        setupListeners()
    }
    protected abstract fun provideBinding(): VB
    protected open fun setupUI() {}
    protected open fun initData() {}
    protected open fun setupObservers() {}
    protected open fun setupListeners() {}
    protected open fun setupInset(topView: View = binding.root, bottomView: View = binding.root){
        ViewCompat.setOnApplyWindowInsetsListener(topView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topView.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val params = bottomView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(
                systemBars.left,
                0,
                systemBars.right,
                systemBars.bottom
            )
            bottomView.layoutParams = params
            insets
        }
    }
}