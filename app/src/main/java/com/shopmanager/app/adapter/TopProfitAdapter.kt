package com.shopmanager.app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopmanager.app.R
import com.shopmanager.app.data.Product
import com.shopmanager.app.databinding.ItemTopProfitBinding
import com.shopmanager.app.util.NumberUtils
import java.io.File

/**
 * 利润 TOP10 榜单 Adapter
 */
class TopProfitAdapter(
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<TopProfitAdapter.VH>() {

    private var items: List<Product> = emptyList()

    fun submitList(list: List<Product>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTopProfitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = items[position]
        holder.bind(position + 1, product)
        holder.itemView.setOnClickListener { onClick(product) }
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemTopProfitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rank: Int, p: Product) {
            binding.tvRank.text = rank.toString()
            // 前三名高亮 primary，其余次要色
            binding.tvRank.setTextColor(
                if (rank <= 3) Color.parseColor("#E85D4A")
                else Color.parseColor("#666666")
            )

            binding.tvName.text = p.name
            binding.tvProfitRate.text = "利润率 ${NumberUtils.formatPercent(p.profitRate)}"
            binding.tvProfitAmount.text = NumberUtils.formatProfit(p.profitAmount)

            if (!p.imagePath.isNullOrEmpty()) {
                Glide.with(binding.ivThumb.context)
                    .load(File(p.imagePath))
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .centerCrop()
                    .into(binding.ivThumb)
            } else {
                binding.ivThumb.setImageResource(R.drawable.ic_placeholder_image)
            }
        }
    }
}
