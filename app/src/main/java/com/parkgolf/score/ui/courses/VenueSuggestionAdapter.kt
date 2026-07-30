package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemVenueSuggestionBinding

class VenueSuggestionAdapter(private val onPick: (String) -> Unit) :
    RecyclerView.Adapter<VenueSuggestionAdapter.VH>() {
    private var names: List<String> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<String>) { names = list; notifyDataSetChanged() }

    inner class VH(val b: ItemVenueSuggestionBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVenueSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = names.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.tvVenueName.text = names[position]
        holder.b.root.setOnClickListener { onPick(names[position]) }
    }
}
