package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemHoleCardBinding

class HoleCardAdapter(
    private val onPar: (index: Int, newValue: Int) -> Unit,
    private val onDelete: (index: Int) -> Unit,
) : RecyclerView.Adapter<HoleCardAdapter.VH>() {
    private var pars: List<Int> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Int>) { pars = list; notifyDataSetChanged() }

    inner class VH(val b: ItemHoleCardBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHoleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = pars.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        holder.b.tvHoleBadge.text = (position + 1).toString()
        holder.b.tvHoleName.text = ctx.getString(R.string.hole_label, position + 1)
        holder.b.tvPar.text = pars[position].toString()
        holder.b.btnParMinus.setOnClickListener { onPar(position, pars[position] - 1) }
        holder.b.btnParPlus.setOnClickListener { onPar(position, pars[position] + 1) }
        holder.b.btnDeleteHole.isEnabled = pars.size > 1
        holder.b.btnDeleteHole.alpha = if (pars.size > 1) 1f else 0.3f
        holder.b.btnDeleteHole.setOnClickListener { onDelete(position) }
    }
}
