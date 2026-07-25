package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemParEditorBinding

class ParEditorAdapter : RecyclerView.Adapter<ParEditorAdapter.VH>() {
    val pars = mutableListOf<Int>()

    @SuppressLint("NotifyDataSetChanged")
    fun setHoleCount(n: Int) {
        while (pars.size < n) pars.add(3)
        while (pars.size > n) pars.removeAt(pars.size - 1)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemParEditorBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemParEditorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = pars.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.tvHoleLabel.text = holder.itemView.context.getString(R.string.hole_number, position + 1)
        holder.b.tvPar.text = pars[position].toString()
        holder.b.btnParMinus.setOnClickListener {
            if (pars[position] > 1) { pars[position] = pars[position] - 1; notifyItemChanged(position) }
        }
        holder.b.btnParPlus.setOnClickListener {
            pars[position] = pars[position] + 1; notifyItemChanged(position)
        }
    }
}
