package com.shopmanager.app.ui.editor

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.shopmanager.app.R
import com.shopmanager.app.data.Product
import com.shopmanager.app.databinding.FragmentEditorBinding
import com.shopmanager.app.util.ImageUtils
import com.shopmanager.app.util.NumberUtils
import com.shopmanager.app.util.addClickScaleAnimation
import com.shopmanager.app.util.gone
import com.shopmanager.app.util.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductEditorFragment : Fragment(R.layout.fragment_editor) {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProductEditorViewModel

    private var tempCameraUri: android.net.Uri? = null

    // 相册选图
    private val pickImageLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            uri?.let { compressAndLoad(it) }
        }

    // 相机拍照
    private val takePictureLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempCameraUri?.let { compressAndLoad(it) }
            }
        }

    // 价格输入：最多两位小数
    private val priceInputFilter = InputFilter { source, _, _, dest, dstart, dend ->
        val newText = StringBuilder(dest).replace(dstart, dend, source.toString()).toString()
        if (newText.matches(Regex("^\\d{0,8}(\\.\\d{0,2})?$"))) null else ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditorBinding.bind(view)

        viewModel = ViewModelProvider(this)[ProductEditorViewModel::class.java]

        val productId = arguments?.getLong("productId", -1L) ?: -1L
        viewModel.loadProduct(productId)

        setupToolbar()
        setupImagePicker()
        setupPriceWatchers()
        setupSaveButton()

        viewModel.currentProduct.observe(viewLifecycleOwner) { product ->
            product?.let { fillForm(it) }
        }
        viewModel.isEdit.observe(viewLifecycleOwner) { isEdit ->
            binding.tvEditorTitle.text = if (isEdit) "编辑商品" else getString(R.string.add_product)
            if (isEdit) binding.btnDelete.visible() else binding.btnDelete.gone()
        }
        viewModel.savedEvent.observe(viewLifecycleOwner) { saved ->
            if (saved) findNavController().popBackStack()
        }
        viewModel.deletedEvent.observe(viewLifecycleOwner) { deleted ->
            if (deleted) findNavController().popBackStack()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.addClickScaleAnimation()
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnDelete.addClickScaleAnimation()
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun setupImagePicker() {
        binding.ivProductImage.addClickScaleAnimation()
        binding.ivProductImage.setOnClickListener { showImagePicker() }
        binding.btnChangeImage.addClickScaleAnimation()
        binding.btnChangeImage.setOnClickListener { showImagePicker() }
    }

    private fun showImagePicker() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.dialog_image_picker, null)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_bottom_sheet)

        content.findViewById<View>(R.id.btnTakePhoto).setOnClickListener {
            dialog.dismiss()
            openCamera()
        }
        content.findViewById<View>(R.id.btnChooseAlbum).setOnClickListener {
            dialog.dismiss()
            openAlbum()
        }
        content.findViewById<View>(R.id.btnCancelPicker).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun openCamera() {
        val pair = ImageUtils.createImageFile(requireContext())
        tempCameraUri = pair.second
        takePictureLauncher.launch(pair.second)
    }

    private fun openAlbum() {
        pickImageLauncher.launch("image/*")
    }

    private fun compressAndLoad(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val path = ImageUtils.compressAndSave(requireContext(), uri)
            withContext(Dispatchers.Main) {
                viewModel.setImagePath(path)
                ImageUtils.loadInto(binding.ivProductImage, path)
            }
        }
    }

    private fun setupPriceWatchers() {
        binding.etCostPrice.filters = arrayOf(priceInputFilter)
        binding.etSalePrice.filters = arrayOf(priceInputFilter)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updateProfit()
        }
        binding.etCostPrice.addTextChangedListener(watcher)
        binding.etSalePrice.addTextChangedListener(watcher)
    }

    private fun updateProfit() {
        val cost = binding.etCostPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val sale = binding.etSalePrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val profit = sale - cost
        val rate = if (cost > 0) (sale - cost) / cost * 100.0 else 0.0
        binding.tvProfitAmount.text = "利润额：${NumberUtils.formatProfit(profit)}"
        binding.tvProfitRate.text = "利润率：${NumberUtils.formatPercent(rate)}"
    }

    private fun setupSaveButton() {
        binding.btnSave.addClickScaleAnimation()
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString().orEmpty().trim()
            if (name.isEmpty()) {
                binding.tilName.error = "请输入商品名称"
                return@setOnClickListener
            }
            binding.tilName.error = null

            val desc = binding.etDesc.text?.toString().orEmpty().trim()
            val cost = binding.etCostPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
            val sale = binding.etSalePrice.text?.toString()?.toDoubleOrNull() ?: 0.0
            val category = binding.etCategory.text?.toString().orEmpty().trim()
            val stock = binding.etStock.text?.toString()?.toIntOrNull() ?: 0
            val remark = binding.etRemark.text?.toString().orEmpty().trim()

            viewModel.saveProduct(name, desc, cost, sale, category, stock, remark)
        }
    }

    private fun fillForm(p: Product) {
        binding.etName.setText(p.name)
        binding.etDesc.setText(p.description)
        binding.etCostPrice.setText(NumberUtils.formatPriceInput(p.costPrice.toString()))
        binding.etSalePrice.setText(NumberUtils.formatPriceInput(p.salePrice.toString()))
        binding.etCategory.setText(p.category)
        binding.etStock.setText(p.stock.toString())
        binding.etRemark.setText(p.remark)
        ImageUtils.loadInto(binding.ivProductImage, p.imagePath)
        updateProfit()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("删除商品")
            .setMessage("确定删除该商品吗？此操作不可恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> viewModel.deleteProduct() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
