package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemParEditorBinding

/** Read-only view of pars; every +/- tap calls [onParChange] with the new value. */
class WizardParAdapter(private val onParChange: (index: Int, newValue: Int) -> Unit) :
    RecyclerView.Adapter<WizardParAdapter.VH>() {

    private var pars: List<Int> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Int>) { pars = list; notifyDataSetChanged() }

    inner class VH(val b: ItemParEditorBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemParEditorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = pars.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        holder.b.tvHoleLabel.text = ctx.getString(R.string.hole_number, position + 1)
        holder.b.tvPar.text = pars[position].toString()
        holder.b.btnParMinus.setOnClickListener { onParChange(position, pars[position] - 1) }
        holder.b.btnParPlus.setOnClickListener { onParChange(position, pars[position] + 1) }
    }
}
