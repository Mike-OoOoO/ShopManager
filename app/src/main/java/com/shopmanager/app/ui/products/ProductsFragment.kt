package com.shopmanager.app.ui.products

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.shopmanager.app.R
import com.shopmanager.app.adapter.ProductAdapter
import com.shopmanager.app.data.Product
import com.shopmanager.app.data.ProductRepository
import com.shopmanager.app.databinding.FragmentProductsBinding
import com.shopmanager.app.util.addClickScaleAnimation

class ProductsFragment : Fragment(R.layout.fragment_products) {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProductsViewModel
    private lateinit var adapter: ProductAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    // 排序选项与枚举的映射
    private val sortOptions = linkedMapOf(
        ProductRepository.SortBy.TIME_DESC to "最新创建",
        ProductRepository.SortBy.PRICE_ASC to "价格从低到高",
        ProductRepository.SortBy.PRICE_DESC to "价格从高到低",
        ProductRepository.SortBy.PROFIT_DESC to "利润从高到低",
        ProductRepository.SortBy.PROFIT_ASC to "利润从低到高"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductsBinding.bind(view)

        viewModel = ViewModelProvider(this)[ProductsViewModel::class.java]

        adapter = ProductAdapter(
            onProductClick = { product -> openEditor(product.id) },
            onProductLongClick = { product -> showQuickActions(product) }
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.itemAnimator = DefaultItemAnimator()
        binding.rvProducts.adapter = adapter

        setupSearch()
        setupButtons()

        viewModel.products.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val empty = list.isEmpty()
            binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvProducts.visibility = if (empty) View.GONE else View.VISIBLE
        }

        viewModel.isCardView.observe(viewLifecycleOwner) { isCard ->
            adapter.setCardView(isCard)
            binding.btnViewToggle.setImageResource(
                if (isCard) R.drawable.ic_grid else R.drawable.ic_list
            )
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val kw = s?.toString().orEmpty()
                searchRunnable = Runnable { viewModel.search(kw) }
                searchHandler.postDelayed(searchRunnable!!, 200)
            }
        })
    }

    private fun setupButtons() {
        binding.btnSort.addClickScaleAnimation()
        binding.btnSort.setOnClickListener { showSortFilterSheet() }

        binding.btnFilter.addClickScaleAnimation()
        binding.btnFilter.setOnClickListener { showSortFilterSheet() }

        binding.btnViewToggle.addClickScaleAnimation()
        binding.btnViewToggle.setOnClickListener { viewModel.toggleView() }

        binding.fabAdd.addClickScaleAnimation()
        binding.fabAdd.setOnClickListener { openEditor(-1L) }
    }

    private fun openEditor(productId: Long) {
        val args = Bundle().apply { putLong("productId", productId) }
        findNavController().navigate(R.id.action_products_to_editor, args)
    }

    private fun openImageViewer(product: Product) {
        val path = product.imagePath ?: return
        val args = Bundle().apply { putString("imagePath", path) }
        findNavController().navigate(R.id.action_products_to_image_viewer, args)
    }

    // ---------- 排序 / 筛选底部面板 ----------

    private fun showSortFilterSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.dialog_sort_filter, null)
        dialog.setContentView(content)
        dialog.applyBottomSheetBackground()

        val rgSort = content.findViewById<RadioGroup>(R.id.rgSort)
        val chipGroup = content.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val btnApply = content.findViewById<View>(R.id.btnApplyFilter)

        // 动态构建排序单选
        rgSort.removeAllViews()
        sortOptions.forEach { (sortBy, label) ->
            val rb = RadioButton(requireContext()).apply {
                text = label
                id = View.generateViewId()
                isChecked = sortBy == viewModel.currentSort
                tag = sortBy
            }
            rgSort.addView(rb)
        }

        // 动态构建分类 chip：全部分类 + 现有分类
        chipGroup.removeAllViews()
        val allChip = Chip(requireContext()).apply {
            text = "全部分类"
            isCheckable = true
            isChecked = viewModel.currentCategory.isEmpty()
            tag = ""
        }
        chipGroup.addView(allChip)
        viewModel.categories.value?.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = cat
                isCheckable = true
                isChecked = cat == viewModel.currentCategory
                tag = cat
            }
            chipGroup.addView(chip)
        }

        btnApplyFilter(btnApply, dialog, rgSort, chipGroup)
        dialog.show()
    }

    private fun btnApplyFilter(
        btnApply: View,
        dialog: BottomSheetDialog,
        rgSort: RadioGroup,
        chipGroup: ChipGroup
    ) {
        btnApply.setOnClickListener {
            val checkedId = rgSort.checkedRadioButtonId
            if (checkedId != View.NO_ID) {
                val rb = rgSort.findViewById<RadioButton>(checkedId)
                (rb.tag as? ProductRepository.SortBy)?.let { viewModel.setSort(it) }
            }
            val checkedChipId = chipGroup.checkedChipId
            val selected = if (checkedChipId != View.NO_ID) {
                chipGroup.findViewById<Chip>(checkedChipId)?.tag as? String ?: ""
            } else ""
            viewModel.setCategory(selected)
            dialog.dismiss()
        }
    }

    // ---------- 长按快捷操作 ----------

    private fun showQuickActions(product: Product) {
        val dialog = BottomSheetDialog(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(requireContext()).apply {
            text = product.name
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 32)
        }
        container.addView(title)

        addActionRow(container, "编辑") {
            dialog.dismiss(); openEditor(product.id)
        }
        addActionRow(container, "查看大图") {
            dialog.dismiss(); openImageViewer(product)
        }
        addActionRow(container, "删除") {
            dialog.dismiss(); confirmDelete(product)
        }

        dialog.setContentView(container)
        dialog.applyBottomSheetBackground()
        dialog.show()
    }

    private fun addActionRow(parent: ViewGroup, label: String, onClick: () -> Unit) {
        val row = TextView(requireContext()).apply {
            text = label
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            val pad = 32
            setPadding(pad, pad, pad, pad)
            isClickable = true
        }
        row.setOnClickListener { onClick() }
        parent.addView(row)
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除商品")
            .setMessage("确定删除「${product.name}」吗？此操作不可恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> viewModel.deleteProduct(product) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }

    /** 让 BottomSheet 应用圆角背景（bg_bottom_sheet） */
    private fun BottomSheetDialog.applyBottomSheetBackground() {
        this.window?.setBackgroundDrawableResource(R.drawable.bg_bottom_sheet)
    }
}
