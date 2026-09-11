package com.shopmanager.app.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.shopmanager.app.R
import com.shopmanager.app.adapter.TopProfitAdapter
import com.shopmanager.app.data.Product
import com.shopmanager.app.databinding.FragmentHomeBinding
import com.shopmanager.app.util.NumberUtils
import com.shopmanager.app.util.gone
import com.shopmanager.app.util.visible

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var topAdapter: TopProfitAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        topAdapter = TopProfitAdapter { product -> openEditor(product) }

        binding.rvTopProfit.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTopProfit.setHasFixedSize(false)
        binding.rvTopProfit.adapter = topAdapter

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalProductsValue.text = NumberUtils.formatCount(stats.totalCount)
            binding.tvAvgProfitRateValue.text = NumberUtils.formatPercent(stats.avgProfitRate)
            binding.tvInventoryValueValue.text = NumberUtils.formatPrice(stats.inventoryValue)
            binding.tvEstimatedProfitValue.text = NumberUtils.formatProfit(stats.estimatedProfit)
        }

        viewModel.topProducts.observe(viewLifecycleOwner) { list ->
            topAdapter.submitList(list)
            if (list.isEmpty()) {
                binding.tvTopProfitTitle.gone()
                binding.rvTopProfit.gone()
            } else {
                binding.tvTopProfitTitle.visible()
                binding.rvTopProfit.visible()
            }
        }
    }

    private fun openEditor(product: Product) {
        val args = Bundle().apply { putLong("productId", product.id) }
        findNavController().navigate(R.id.action_home_to_editor, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
