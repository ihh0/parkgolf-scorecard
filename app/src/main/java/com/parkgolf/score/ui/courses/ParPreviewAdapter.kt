package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemParPreviewBinding

class ParPreviewAdapter : RecyclerView.Adapter<ParPreviewAdapter.VH>() {
    private var pars: List<Int> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Int>) { pars = list; notifyDataSetChanged() }

    inner class VH(val b: ItemParPreviewBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemParPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = pars.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.tvIndex.text = (position + 1).toString()
        holder.b.tvPar.text = pars[position].toString()
    }
}
