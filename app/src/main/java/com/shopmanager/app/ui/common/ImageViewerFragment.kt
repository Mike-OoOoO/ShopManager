package com.shopmanager.app.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.shopmanager.app.R
import com.shopmanager.app.databinding.FragmentImageViewerBinding
import com.shopmanager.app.util.addClickScaleAnimation
import java.io.File

/**
 * 全屏图片查看器：黑色背景，点击关闭按钮或图片关闭
 */
class ImageViewerFragment : Fragment(R.layout.fragment_image_viewer) {

    private var _binding: FragmentImageViewerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageViewerBinding.bind(view)

        val imagePath = arguments?.getString("imagePath").orEmpty()

        if (imagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(imagePath))
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .fitCenter()
                .into(binding.ivFullImage)
        } else {
            binding.ivFullImage.setImageResource(R.drawable.ic_placeholder_image)
        }

        binding.btnCloseImage.addClickScaleAnimation()
        binding.btnCloseImage.setOnClickListener { findNavController().popBackStack() }

        // 点击图片也关闭
        binding.ivFullImage.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
