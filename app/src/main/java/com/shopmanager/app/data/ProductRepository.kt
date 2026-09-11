package com.shopmanager.app.data

import kotlinx.coroutines.flow.Flow

/**
 * 商品数据仓库
 * 排序、筛选在仓库层完成（利润为计算属性，不适合 SQL 排序）
 */
class ProductRepository(private val dao: ProductDao) {

    fun getAllFlow(): Flow<List<Product>> = dao.getAllFlow()

    suspend fun getAll(): List<Product> = dao.getAll()

    suspend fun getById(id: Long): Product? = dao.getById(id)

    suspend fun insert(product: Product): Long = dao.insert(product)

    suspend fun update(product: Product) = dao.update(product)

    suspend fun delete(product: Product) = dao.delete(product)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun insertAll(products: List<Product>) = dao.insertAll(products)

    suspend fun searchByName(query: String): List<Product> = dao.searchByName(query)

    suspend fun getAllCategories(): List<String> = dao.getAllCategories()

    suspend fun getCount(): Int = dao.getCount()

    suspend fun getTotalInventoryValue(): Double = dao.getTotalInventoryValue()

    suspend fun getTotalEstimatedProfit(): Double = dao.getTotalEstimatedProfit()

    /**
     * 获取利润排名前 N 的商品
     */
    suspend fun getTopProfit(limit: Int = 10): List<Product> {
        return dao.getAll().sortedByDescending { it.profitAmount }.take(limit)
    }

    /**
     * 平均利润率（按商品数平均）
     */
    suspend fun getAverageProfitRate(): Double {
        val all = dao.getAll()
        if (all.isEmpty()) return 0.0
        return all.sumOf { it.profitRate } / all.size
    }

    /**
     * 综合查询：关键词搜索 + 分类筛选 + 排序
     */
    suspend fun queryProducts(
        keyword: String = "",
        category: String = "",
        sortBy: SortBy = SortBy.TIME_DESC
    ): List<Product> {
        val list = if (keyword.isNotBlank()) {
            dao.searchByName(keyword.trim())
        } else {
            dao.getAll()
        }
        val filtered = if (category.isNotBlank()) {
            list.filter { it.category == category }
        } else list
        return when (sortBy) {
            SortBy.TIME_DESC -> filtered.sortedByDescending { it.createdAt }
            SortBy.PRICE_ASC -> filtered.sortedBy { it.salePrice }
            SortBy.PRICE_DESC -> filtered.sortedByDescending { it.salePrice }
            SortBy.PROFIT_DESC -> filtered.sortedByDescending { it.profitAmount }
            SortBy.PROFIT_ASC -> filtered.sortedBy { it.profitAmount }
        }
    }

    enum class SortBy {
        TIME_DESC, PRICE_ASC, PRICE_DESC, PROFIT_DESC, PROFIT_ASC
    }
}
