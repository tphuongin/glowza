package com.sgroupmobile.glowza.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB: ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding?: throw IllegalStateException("Binding is not initialized")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = provideBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupUI()
        setupObservers()
        setupListeners()
    }
    abstract fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VB

    protected open fun setupUI() {}
    protected open fun initData() {}
    protected open fun setupObservers() {}
    protected open fun setupListeners() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}