package com.shopmanager.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.shopmanager.app.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV 导出工具
 * - 导出到 cacheDir/exports/，UTF-8 BOM 开头（Excel 兼容中文）
 * - 列：商品名称,商品介绍,进价,售价,利润额,利润率,商品分类,库存数量,备注,创建时间
 * - 通过 Intent.ACTION_SEND 分享，FileProvider 授权读取
 */
object CsvExporter {

    /** UTF-8 BOM，用于 Excel 正确识别中文 */
    private const val BOM = "\uFEFF"
    private const val EXPORT_DIR = "exports"
    private const val MIME_TYPE = "text/csv"

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val FILE_NAME_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 导出商品列表为 CSV 文件到 cacheDir/exports/，返回生成的文件
     */
    suspend fun exportToFile(context: Context, products: List<Product>): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
            val file = File(dir, "商品列表_${FILE_NAME_FORMAT.format(Date())}.csv")
            // 以 UTF-8 BOM 开头，Excel 打开中文不乱码
            file.writeText(BOM + toCsv(products), Charsets.UTF_8)
            file
        }

    /** 生成 CSV 内容字符串（不含 BOM） */
    fun toCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("商品名称,商品介绍,进价,售价,利润额,利润率,商品分类,库存数量,备注,创建时间\n")
        for (p in products) {
            sb.append(escape(p.name)).append(',')
            sb.append(escape(p.description)).append(',')
            sb.append(String.format(Locale.getDefault(), "%.2f", p.costPrice)).append(',')
            sb.append(String.format(Locale.getDefault(), "%.2f", p.salePrice)).append(',')
            sb.append(String.format(Locale.getDefault(), "%.2f", p.profitAmount)).append(',')
            sb.append(String.format(Locale.getDefault(), "%.1f%%", p.profitRate)).append(',')
            sb.append(escape(p.category)).append(',')
            sb.append(p.stock).append(',')
            sb.append(escape(p.remark)).append(',')
            sb.append(escape(DATE_FORMAT.format(Date(p.createdAt)))).append('\n')
        }
        return sb.toString()
    }

    /** 字段含逗号、双引号或换行时用双引号包裹，内部双引号转义为两个双引号 */
    private fun escape(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        return if (needsQuote) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /** 分享 CSV 文件 */
    fun shareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "分享CSV文件")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
