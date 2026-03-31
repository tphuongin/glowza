package com.sgroupmobile.glowza.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sgroupmobile.glowza.R
import com.sgroupmobile.glowza.base.BaseFragment
import com.sgroupmobile.glowza.common.enums.Constants.EXTRA_IMAGE_URI
import com.sgroupmobile.glowza.common.enums.GalleryMode
import com.sgroupmobile.glowza.common.enums.GalleryTab
import com.sgroupmobile.glowza.data.model.GalleryItem
import com.sgroupmobile.glowza.databinding.FragmentGalleryBinding
import com.sgroupmobile.glowza.ui.photo_editor.activity.EditorActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryFragment : BaseFragment<FragmentGalleryBinding>() {

    private lateinit var galleryAdapter: GalleryAdapter
    private val viewModel: GalleryViewModel by activityViewModels()

    companion object {
        private const val ARG_GALLERY_TAB = "arg_gallery_tab"

        fun newInstance(category: GalleryTab): GalleryFragment {
            val fragment = GalleryFragment()
            val args = Bundle().apply {
                putString(ARG_GALLERY_TAB, category.name)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun provideBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentGalleryBinding.inflate(inflater, container, false)

    override fun initData() {
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(
            mode = GalleryMode.SINGLE,
            onSingleClick = { item ->
                if(!item.isVideo) openEditorWithImage(item)
                else {
                    Toast.makeText(requireContext(), "Vui lòng chọn ảnh", Toast.LENGTH_LONG).show()
                }
            },
            onMultiChange = {
            }
        )
        binding.rvGallery.apply {
            adapter = galleryAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeData() {
        val tabName = arguments?.getString(ARG_GALLERY_TAB) ?: return
        val category = GalleryTab.valueOf(tabName)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val flowToObserve = when (category) {
                    GalleryTab.ALL -> viewModel.images
                    GalleryTab.FAVOURITE -> viewModel.favoritePhotos
                    GalleryTab.GLOWZA -> viewModel.glowzaPhotos
                    GalleryTab.VIDEO -> viewModel.videos
                }

                flowToObserve.collect { photos ->
                    // 1. Chặn ngay từ đầu nếu Fragment đang trong quá trình bị hủy
                    if (!isAdded || view == null) return@collect

                    val layoutAnim = AnimationUtils.loadLayoutAnimation(
                        requireContext(),
                        R.anim.layout_animation_slide_up
                    )

                    binding.rvGallery.layoutAnimation = layoutAnim

                    galleryAdapter.submitList(photos) {
                        if (isAdded && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && view != null) {
                            try {
                                binding.rvGallery.scheduleLayoutAnimation()
                            } catch (e: Exception) {
                                Log.e("GalleryFragment", "Bỏ qua animation vì view đang bị hủy")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openEditorWithImage(item: GalleryItem) {
        if (item.isVideo) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Không tìm thấy trình phát video", Toast.LENGTH_SHORT).show()
            }


        } else {
            // Mở Editor xử lý Ảnh
            val intent = Intent(requireContext(), EditorActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, item.uri.toString())
            }
            startActivity(intent)
        }
    }
}