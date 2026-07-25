package com.parkgolf.score.ui.courses

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemVenueBinding

data class VenueRow(val venueId: Long, val name: String, val courseSummary: String)

class VenueAdapter(private val onClick: (Long) -> Unit) :
    RecyclerView.Adapter<VenueAdapter.VH>() {
    private val items = mutableListOf<VenueRow>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<VenueRow>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemVenueBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVenueBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.b.tvVenueName.text = row.name
        holder.b.tvCourseSummary.text = row.courseSummary
        holder.b.root.setOnClickListener { onClick(row.venueId) }
    }
}
