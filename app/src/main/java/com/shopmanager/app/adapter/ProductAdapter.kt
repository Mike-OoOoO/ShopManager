package com.shopmanager.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopmanager.app.R
import com.shopmanager.app.data.Product
import com.shopmanager.app.databinding.ItemProductCardBinding
import com.shopmanager.app.databinding.ItemProductListBinding
import com.shopmanager.app.util.NumberUtils
import java.io.File

/**
 * 商品列表 Adapter：同时支持卡片视图与简洁列表视图
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onProductLongClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isCardView: Boolean = true
    private var items: List<Product> = emptyList()

    companion object {
        private const val TYPE_CARD = 0
        private const val TYPE_LIST = 1
    }

    fun submitList(list: List<Product>) {
        items = list
        notifyDataSetChanged()
    }

    fun setCardView(isCard: Boolean) {
        if (isCardView == isCard) return
        isCardView = isCard
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isCardView) TYPE_CARD else TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_LIST -> ListVH(ItemProductListBinding.inflate(inflater, parent, false))
            else -> CardVH(ItemProductCardBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val product = items[position]
        when (holder) {
            is CardVH -> holder.bind(product)
            is ListVH -> holder.bind(product)
        }
        holder.itemView.setOnClickListener { onProductClick(product) }
        holder.itemView.setOnLongClickListener {
            onProductLongClick(product)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    private fun loadThumb(path: String?, into: android.widget.ImageView) {
        if (!path.isNullOrEmpty()) {
            Glide.with(into.context)
                .load(File(path))
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .centerCrop()
                .into(into)
        } else {
            into.setImageResource(R.drawable.ic_placeholder_image)
        }
    }

    /** 卡片视图 ViewHolder */
    inner class CardVH(private val binding: ItemProductCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(p: Product) {
            binding.tvName.text = p.name
            val cat = if (p.category.isBlank()) "未分类" else p.category
            binding.tvCategory.text = "$cat · 库存 ${p.stock}"
            binding.tvSalePrice.text = NumberUtils.formatPrice(p.salePrice)
            binding.tvProfit.text = "利润 ${NumberUtils.formatProfit(p.profitAmount)}"
            loadThumb(p.imagePath, binding.ivThumb)
        }
    }

    /** 简洁列表视图 ViewHolder */
    inner class ListVH(private val binding: ItemProductListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(p: Product) {
            binding.tvName.text = p.name
            val cat = if (p.category.isBlank()) "未分类" else p.category
            binding.tvMeta.text = "$cat · 库存 ${p.stock}"
            binding.tvSalePrice.text = NumberUtils.formatPrice(p.salePrice)
            binding.tvProfit.text = NumberUtils.formatProfit(p.profitAmount)
            loadThumb(p.imagePath, binding.ivThumb)
        }
    }
}
