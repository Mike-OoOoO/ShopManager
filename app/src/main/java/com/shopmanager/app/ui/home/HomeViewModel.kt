package com.shopmanager.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmanager.app.App
import com.shopmanager.app.data.Product
import com.shopmanager.app.data.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 首页统计数据
 */
data class StatsData(
    val totalCount: Int = 0,
    val avgProfitRate: Double = 0.0,
    val inventoryValue: Double = 0.0,
    val estimatedProfit: Double = 0.0
)

class HomeViewModel : ViewModel() {

    private val repo: ProductRepository =
        ProductRepository(App.instance.database.productDao())

    private val _stats = MutableLiveData(StatsData())
    val stats: LiveData<StatsData> = _stats

    private val _topProducts = MutableLiveData<List<Product>>(emptyList())
    val topProducts: LiveData<List<Product>> = _topProducts

    init {
        viewModelScope.launch {
            repo.getAllFlow()
                .catch { emit(emptyList()) }
                .collect { list ->
                    // 在 IO 上做纯计算，避免阻塞主线程
                    launch(Dispatchers.IO) {
                        val count = list.size
                        val avgRate = if (list.isEmpty()) 0.0
                        else list.sumOf { it.profitRate } / list.size
                        val invValue = list.sumOf { it.inventoryValue }
                        val estProfit = list.sumOf { it.estimatedProfit }
                        _stats.postValue(
                            StatsData(count, avgRate, invValue, estProfit)
                        )
                        _topProducts.postValue(
                            list.sortedByDescending { it.profitAmount }.take(10)
                        )
                    }
                }
        }
    }
}
