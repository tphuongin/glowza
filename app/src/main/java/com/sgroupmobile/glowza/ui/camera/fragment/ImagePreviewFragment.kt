package com.sgroupmobile.glowza.ui.camera.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.databinding.FragmentImagePreviewBinding
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.sgroupmobile.glowza.util.saveImageToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import com.sgroupmobile.glowza.util.showFancySnackbar

class ImagePreviewFragment : BaseFragment<FragmentImagePreviewBinding>() {
    private var uri: Uri? = null
    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentImagePreviewBinding = FragmentImagePreviewBinding.inflate(inflater, container, false)

    override fun initData() {
        super.initData()
        val uriString = arguments?.getString("imageUri")

        if (uriString != null) {
            uri = uriString.toUri()
            binding.ivPhoto.setImageURI(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnDownload.setOnClickListener {
            if(uri == null) return@setOnClickListener
            val fileName = "Glowza_${System.currentTimeMillis()}"

            // Chạy ngầm để không đơ máy
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val resultUri = saveImageToGallery(requireContext(), uri!!, fileName)

                withContext(Dispatchers.Main) {
                    if (resultUri != null) {
                        // Rung nhẹ
                        binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        showFancySnackbar(getString(R.string.save_success), binding.root)
                    } else {
                        showFancySnackbar(getString(R.string.save_error), binding.root)
                    }
                }
            }
        }
        binding.btnEdit.setOnClickListener {
            uri?.let { imageUri ->
                val intent = Intent(requireContext(), EditorActivity::class.java).apply {
                    putExtra(com.sgroupmobile.glowza.common.enums.Constants.EXTRA_IMAGE_URI, imageUri.toString())
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

}