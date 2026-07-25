package com.parkgolf.score.ui.start

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemRecentCourseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentCourseAdapter(private val onClick: (RecentCourse) -> Unit) :
    RecyclerView.Adapter<RecentCourseAdapter.VH>() {
    private val items = mutableListOf<RecentCourse>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<RecentCourse>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemRecentCourseBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRecentCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvVenue.text = "${item.venueName} · ${item.holeCount}홀"
        holder.b.tvSub.text = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(item.lastPlayed))
        holder.b.root.setOnClickListener { onClick(item) }
    }
}
