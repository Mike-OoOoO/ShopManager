package com.shopmanager.app.ui.editor

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
 * 商品编辑 ViewModel：新增 / 编辑 / 删除
 */
class ProductEditorViewModel : ViewModel() {

    private val repo: ProductRepository =
        ProductRepository(App.instance.database.productDao())

    private val _currentProduct = MutableLiveData<Product?>(null)
    val currentProduct: LiveData<Product?> = _currentProduct

    private val _isEdit = MutableLiveData(false)
    val isEdit: LiveData<Boolean> = _isEdit

    /** 保存成功事件 */
    private val _savedEvent = MutableLiveData<Boolean>()
    val savedEvent: LiveData<Boolean> = _savedEvent

    /** 删除成功事件 */
    private val _deletedEvent = MutableLiveData<Boolean>()
    val deletedEvent: LiveData<Boolean> = _deletedEvent

    /** 已选择的图片路径（压缩保存后） */
    var imagePath: String? = null
        private set

    fun setImagePath(path: String?) {
        imagePath = path
    }

    fun loadProduct(id: Long) {
        if (id < 0) {
            _currentProduct.postValue(null)
            _isEdit.postValue(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repo.getById(id)
            _currentProduct.postValue(p)
            _isEdit.postValue(p != null)
            if (p != null) imagePath = p.imagePath
        }
    }

    fun saveProduct(
        name: String,
        description: String,
        costPrice: Double,
        salePrice: Double,
        category: String,
        stock: Int,
        remark: String
    ) {
        val existing = _currentProduct.value
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val product = if (existing != null) {
                existing.copy(
                    name = name,
                    description = description,
                    imagePath = imagePath ?: existing.imagePath,
                    costPrice = costPrice,
                    salePrice = salePrice,
                    category = category,
                    stock = stock,
                    remark = remark,
                    updatedAt = now
                )
            } else {
                Product(
                    name = name,
                    description = description,
                    imagePath = imagePath,
                    costPrice = costPrice,
                    salePrice = salePrice,
                    category = category,
                    stock = stock,
                    remark = remark,
                    createdAt = now,
                    updatedAt = now
                )
            }
            if (existing != null) {
                repo.update(product)
            } else {
                repo.insert(product)
            }
            _savedEvent.postValue(true)
        }
    }

    fun deleteProduct() {
        val existing = _currentProduct.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(existing)
            _deletedEvent.postValue(true)
        }
    }
}
