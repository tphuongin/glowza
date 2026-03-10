package com.sgroupmobile.glowza.base

import android.os.Bundle
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
    protected open fun setupInset(){
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}