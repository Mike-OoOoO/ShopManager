package com.shopmanager.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchByName(query: String): List<Product>

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int

    @Query("SELECT COALESCE(SUM(costPrice * stock), 0) FROM products")
    suspend fun getTotalInventoryValue(): Double

    @Query("SELECT COALESCE(SUM((salePrice - costPrice) * stock), 0) FROM products")
    suspend fun getTotalEstimatedProfit(): Double
}
