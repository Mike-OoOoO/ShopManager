package com.shopmanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 商品实体
 * 利润额、利润率为计算属性，不持久化
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val imagePath: String? = null,
    val costPrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val category: String = "",
    val stock: Int = 0,
    val remark: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 利润额 = 售价 - 进价 */
    val profitAmount: Double
        get() = salePrice - costPrice

    /** 利润率 = (售价 - 进价) / 进价 × 100%，进价为 0 时返回 0 */
    val profitRate: Double
        get() = if (costPrice > 0) (salePrice - costPrice) / costPrice * 100.0 else 0.0

    /** 库存总价值 = 进价 × 库存 */
    val inventoryValue: Double
        get() = costPrice * stock

    /** 预估总利润 = 利润额 × 库存 */
    val estimatedProfit: Double
        get() = profitAmount * stock
}
