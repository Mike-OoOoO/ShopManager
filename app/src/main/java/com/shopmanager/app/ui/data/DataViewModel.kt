package com.shopmanager.app.ui.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmanager.app.App
import com.shopmanager.app.data.Product
import com.shopmanager.app.data.ProductRepository
import com.shopmanager.app.util.BackupManager
import com.shopmanager.app.util.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 数据管理 ViewModel：备份 / 恢复 / 导出 CSV
 */
class DataViewModel : ViewModel() {

    private val repo: ProductRepository =
        ProductRepository(App.instance.database.productDao())

    /** 备份结果事件（true 成功，message 为提示文案） */
    private val _backupResult = MutableLiveData<Pair<Boolean, String>>()
    val backupResult: LiveData<Pair<Boolean, String>> = _backupResult

    /** 恢复结果事件（恢复条数 > 0 视为成功） */
    private val _restoreResult = MutableLiveData<Pair<Boolean, Int>>()
    val restoreResult: LiveData<Pair<Boolean, Int>> = _restoreResult

    /** 导出完成事件（CSV 文件） */
    private val _exportResult = MutableLiveData<File?>()
    val exportResult: LiveData<File?> = _exportResult

    /** 是否进行中（用于防止重复点击） */
    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    fun backup(destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _busy.postValue(true)
            try {
                val name = BackupManager.backup(App.instance, destinationUri)
                _backupResult.postValue(true to name)
            } catch (e: Exception) {
                _backupResult.postValue(false to (e.message ?: "备份失败"))
            } finally {
                _busy.postValue(false)
            }
        }
    }

    fun restore(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _busy.postValue(true)
            try {
                val count = BackupManager.restore(App.instance, sourceUri)
                _restoreResult.postValue(true to count)
            } catch (e: Exception) {
                _restoreResult.postValue(false to 0)
            } finally {
                _busy.postValue(false)
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            _busy.postValue(true)
            try {
                val products: List<Product> = repo.getAll()
                val file = CsvExporter.exportToFile(App.instance, products)
                _exportResult.postValue(file)
            } catch (e: Exception) {
                _exportResult.postValue(null)
            } finally {
                _busy.postValue(false)
            }
        }
    }

    fun loadAllProducts(context: Context, onLoaded: (List<Product>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.getAll()
            kotlinx.coroutines.withContext(Dispatchers.Main) { onLoaded(list) }
        }
    }
}
