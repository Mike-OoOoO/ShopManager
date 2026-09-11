package com.shopmanager.app.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmanager.app.App
import com.shopmanager.app.data.Product
import com.shopmanager.app.data.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 商品列表 ViewModel：搜索 / 排序 / 分类筛选 / 视图切换 / 删除
 */
class ProductsViewModel : ViewModel() {

    private val repo: ProductRepository =
        ProductRepository(App.instance.database.productDao())

    private val _products = MutableLiveData<List<Product>>(emptyList())
    val products: LiveData<List<Product>> = _products

    private val _categories = MutableLiveData<List<String>>(emptyList())
    val categories: LiveData<List<String>> = _categories

    private val _isCardView = MutableLiveData(true)
    val isCardView: LiveData<Boolean> = _isCardView

    var currentKeyword: String = ""
        private set
    var currentCategory: String = ""
        private set
    var currentSort: ProductRepository.SortBy = ProductRepository.SortBy.TIME_DESC
        private set

    init {
        refresh()
        refreshCategories()
    }

    fun search(keyword: String) {
        currentKeyword = keyword
        refresh()
    }

    fun setCategory(category: String) {
        currentCategory = category
        refresh()
    }

    fun setSort(sort: ProductRepository.SortBy) {
        currentSort = sort
        refresh()
    }

    fun toggleView() {
        _isCardView.value = (_isCardView.value ?: true).not()
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(product)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.queryProducts(currentKeyword, currentCategory, currentSort)
            _products.postValue(list)
        }
    }

    private fun refreshCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            _categories.postValue(repo.getAllCategories())
        }
    }
}
