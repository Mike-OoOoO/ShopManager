package com.shopmanager.app.util

import android.content.Context
import android.net.Uri
import com.shopmanager.app.App
import com.shopmanager.app.data.Product
import com.shopmanager.app.data.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份 / 恢复工具
 * - 备份：将所有商品序列化为 data.json（JSON 数组），并把商品图片打包进 images/，写入 SAF Uri
 * - 恢复：从 SAF Uri 读取 zip，解析 data.json，复制图片到私有目录，清空现有数据后批量插入
 * - 不访问网络，全部在本地私有目录与 SAF 通道完成
 */
object BackupManager {

    private const val IMAGE_DIR = "product_images"
    private const val DATA_ENTRY = "data.json"
    private const val IMAGES_PREFIX = "images/"

    private fun repository(): ProductRepository =
        ProductRepository(App.instance.database.productDao())

    /**
     * 备份到用户通过 SAF 选择的 destinationUri
     * @return 备份文件的显示名称
     */
    suspend fun backup(context: Context, destinationUri: Uri): String = withContext(Dispatchers.IO) {
        val repo = repository()
        val products = repo.getAll()
        val fileName = generateBackupFileName()

        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            ZipOutputStream(output.buffered()).use { zos ->
                // 1) data.json：每个商品包含所有字段
                val jsonArray = JSONArray()
                for (p in products) {
                    val obj = JSONObject()
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("description", p.description)
                    obj.put("imagePath", p.imagePath ?: JSONObject.NULL)
                    obj.put("costPrice", p.costPrice)
                    obj.put("salePrice", p.salePrice)
                    obj.put("category", p.category)
                    obj.put("stock", p.stock)
                    obj.put("remark", p.remark)
                    obj.put("createdAt", p.createdAt)
                    obj.put("updatedAt", p.updatedAt)
                    jsonArray.put(obj)
                }
                zos.putNextEntry(ZipEntry(DATA_ENTRY))
                zos.write(jsonArray.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2) images/：以原文件名打包所有商品图片
                for (p in products) {
                    val path = p.imagePath ?: continue
                    val file = File(path)
                    if (!file.exists()) continue
                    zos.putNextEntry(ZipEntry(IMAGES_PREFIX + file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        fileName
    }

    /**
     * 从 SAF Uri 恢复
     * @return 恢复的商品数量
     */
    suspend fun restore(context: Context, sourceUri: Uri): Int = withContext(Dispatchers.IO) {
        val imageDir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        var dataJson: String? = null

        // 1) 读取 zip：解析 data.json 并把 images/ 下的文件复制到私有目录
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        when {
                            entry.name == DATA_ENTRY -> {
                                dataJson = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            entry.name.startsWith(IMAGES_PREFIX) -> {
                                val name = entry.name.removePrefix(IMAGES_PREFIX)
                                if (name.isNotEmpty()) {
                                    val outFile = File(imageDir, name)
                                    outFile.outputStream().use { zis.copyTo(it) }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        val json = dataJson ?: return@withContext 0
        val array = JSONArray(json)
        val products = ArrayList<Product>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            // 图片路径以文件名为准重新指向本次恢复后的私有目录
            val rawImagePath = obj.optString("imagePath", "")
            val restoredImagePath = if (rawImagePath.isEmpty()) {
                null
            } else {
                val imageFile = File(imageDir, File(rawImagePath).name)
                if (imageFile.exists()) imageFile.absolutePath else null
            }
            products.add(
                Product(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", ""),
                    description = obj.optString("description", ""),
                    imagePath = restoredImagePath,
                    costPrice = obj.optDouble("costPrice", 0.0),
                    salePrice = obj.optDouble("salePrice", 0.0),
                    category = obj.optString("category", ""),
                    stock = obj.optInt("stock", 0),
                    remark = obj.optString("remark", ""),
                    createdAt = obj.optLong("createdAt", 0L),
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            )
        }

        // 2) 清空现有数据后批量插入
        val repo = repository()
        repo.deleteAll()
        repo.insertAll(products)
        products.size
    }

    /** 生成备份文件名：shop_manager_backup_yyyyMMdd_HHmmss.zip */
    fun generateBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "shop_manager_backup_${sdf.format(Date())}.zip"
    }
}
