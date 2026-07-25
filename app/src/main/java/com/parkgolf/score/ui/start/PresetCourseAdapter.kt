package com.parkgolf.score.ui.start

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.data.db.entity.VenueEntity
import com.parkgolf.score.databinding.ItemPresetCourseBinding

class PresetCourseAdapter(private val onClick: (Long) -> Unit) :
    RecyclerView.Adapter<PresetCourseAdapter.VH>() {
    private val items = mutableListOf<VenueEntity>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<VenueEntity>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemPresetCourseBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPresetCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val v = items[position]
        holder.b.tvPreset.text = v.name
        holder.b.root.setOnClickListener { onClick(v.id) }
    }
}
