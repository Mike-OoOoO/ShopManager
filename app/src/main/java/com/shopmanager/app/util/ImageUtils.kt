package com.shopmanager.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.shopmanager.app.R
import java.io.File
import java.io.FileOutputStream

/**
 * 图片处理工具
 * - 压缩保存：长边不超过 1024px，JPEG 质量 80%，保存到 filesDir/product_images/
 * - 加载：统一通过 Glide 加载，占位图 ic_placeholder_image
 * - 删除：按路径删除私有目录图片
 * - 创建相机临时文件：通过 FileProvider 返回可拍照的 Uri
 */
object ImageUtils {

    private const val TAG = "ImageUtils"
    private const val IMAGE_DIR = "product_images"
    private const val MAX_LONG_EDGE = 1024
    private const val JPEG_QUALITY = 80

    private fun imageDir(context: Context): File =
        File(context.filesDir, IMAGE_DIR).apply { mkdirs() }

    /**
     * 压缩并保存图片到 APP 私有目录，返回保存后的文件绝对路径；失败返回 null
     */
    fun compressAndSave(context: Context, sourceUri: Uri): String? {
        Log.d(TAG, "compressAndSave start: uri=$sourceUri")
        return try {
            // 第一遍：仅读取边界信息，不真正解码
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = context.contentResolver.openInputStream(sourceUri)
            if (boundsStream == null) {
                Log.e(TAG, "openInputStream for bounds returned null, trying direct file path")
                return compressFromFile(context, sourceUri)
            }
            boundsStream.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            Log.d(TAG, "bounds: ${bounds.outWidth}x${bounds.outHeight}, mime=${bounds.outMimeType}")

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.e(TAG, "invalid bounds, trying direct file path")
                return compressFromFile(context, sourceUri)
            }

            // 根据长边计算 inSampleSize，使缩图后长边 <= 1024
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            Log.d(TAG, "inSampleSize=$sampleSize")

            val sampleOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val decodeStream = context.contentResolver.openInputStream(sourceUri)
            if (decodeStream == null) {
                Log.e(TAG, "second openInputStream returned null, trying direct file path")
                return compressFromFile(context, sourceUri)
            }
            val bitmap = decodeStream.use {
                BitmapFactory.decodeStream(it, null, sampleOptions)
            }
            if (bitmap == null) {
                Log.e(TAG, "decodeStream returned null, trying direct file path")
                return compressFromFile(context, sourceUri)
            }
            Log.d(TAG, "decoded bitmap: ${bitmap.width}x${bitmap.height}")

            val outFile = File(imageDir(context), "product_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
                fos.flush()
                Log.d(TAG, "compress result=$ok, file=${outFile.absolutePath}, size=${outFile.length()}")
            }
            bitmap.recycle()

            if (outFile.exists() && outFile.length() > 0) {
                Log.d(TAG, "compressAndSave success: ${outFile.absolutePath}")
                outFile.absolutePath
            } else {
                Log.e(TAG, "output file missing or empty")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "compressAndSave exception", e)
            null
        }
    }

    /**
     * 备选方案：如果 Uri 是 FileProvider content://，尝试直接从文件路径读取
     * FileProvider 的路径格式通常是 content://<authority>/<name>/<relative path>
     */
    private fun compressFromFile(context: Context, sourceUri: Uri): String? {
        return try {
            // 尝试从 Uri 中提取文件路径
            val file = uriToFile(context, sourceUri)
            if (file == null || !file.exists()) {
                Log.e(TAG, "compressFromFile: file not found for uri=$sourceUri")
                return null
            }
            Log.d(TAG, "compressFromFile: direct file path=${file.absolutePath}, size=${file.length()}")

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            Log.d(TAG, "compressFromFile bounds: ${bounds.outWidth}x${bounds.outHeight}")

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.e(TAG, "compressFromFile: invalid bounds")
                return null
            }

            val sampleOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, sampleOptions)
            if (bitmap == null) {
                Log.e(TAG, "compressFromFile: decodeFile returned null")
                return null
            }
            Log.d(TAG, "compressFromFile decoded: ${bitmap.width}x${bitmap.height}")

            val outFile = File(imageDir(context), "product_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
                fos.flush()
            }
            bitmap.recycle()
            Log.d(TAG, "compressFromFile success: ${outFile.absolutePath}")
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "compressFromFile exception", e)
            null
        }
    }

    /**
     * 尝试将 content:// Uri 转换为本地文件路径
     * 对于 FileProvider，路径格式为 content://<authority>/<name>/<relative>
     */
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            if (uri.scheme == "file") {
                return uri.path?.let { File(it) }
            }
            if (uri.scheme == "content") {
                // 尝试通过 contentResolver 查询 _data 列（相册图片）
                context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                        if (colIndex >= 0) {
                            val path = cursor.getString(colIndex)
                            if (!path.isNullOrEmpty()) return File(path)
                        }
                    }
                }
                // 对于 FileProvider，尝试从路径段推断
                // content://com.shopmanager.app.fileprovider/product_images/temp_xxx.jpg
                val segments = uri.pathSegments
                if (segments.size >= 2) {
                    val name = segments[0] // "product_images"
                    val fileName = segments[1] // "temp_xxx.jpg"
                    if (name == "product_images") {
                        return File(imageDir(context), fileName)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "uriToFile exception", e)
            null
        }
    }

    /** 计算采样率：保证解码后最长边不超过 [MAX_LONG_EDGE] */
    private fun calculateInSampleSize(outWidth: Int, outHeight: Int): Int {
        if (outWidth <= 0 || outHeight <= 0) return 1
        var inSampleSize = 1
        val longest = maxOf(outWidth, outHeight)
        while (longest / inSampleSize > MAX_LONG_EDGE) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /** 从私有目录加载图片到 ImageView（Glide）；path 为空时只显示占位图 */
    fun loadInto(imageView: ImageView, path: String?) {
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            Log.d(TAG, "loadInto: path=$path, exists=${file.exists()}, size=${file.length()}")
            Glide.with(imageView.context)
                .load(file)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .centerCrop()
                .into(imageView)
        } else {
            Log.d(TAG, "loadInto: path is null, showing placeholder")
            Glide.with(imageView.context)
                .load(R.drawable.ic_placeholder_image)
                .centerCrop()
                .into(imageView)
        }
    }

    /** 删除商品图片文件（路径为空或文件不存在时静默忽略） */
    fun deleteImage(path: String?) {
        if (path.isNullOrEmpty()) return
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "deleteImage exception", e)
        }
    }

    /**
     * 创建相机临时文件（用于 ActivityResultContracts.TakePicture）
     * authority = context.packageName + ".fileprovider"
     */
    fun createImageFile(context: Context): Pair<File, Uri> {
        val file = File(imageDir(context), "temp_${System.currentTimeMillis()}.jpg")
        // 确保文件存在，某些相机 APP 需要文件已存在
        if (!file.exists()) {
            file.createNewFile()
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        Log.d(TAG, "createImageFile: ${file.absolutePath}, uri=$uri")
        return Pair(file, uri)
    }
}
