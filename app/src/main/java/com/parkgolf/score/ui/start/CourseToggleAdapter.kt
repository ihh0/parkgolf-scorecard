package com.parkgolf.score.ui.start

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.data.db.entity.CourseEntity
import com.parkgolf.score.databinding.ItemCourseToggleBinding

class CourseToggleAdapter(private val onToggle: () -> Unit) :
    RecyclerView.Adapter<CourseToggleAdapter.VH>() {
    private val items = mutableListOf<CourseEntity>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<CourseEntity>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemCourseToggleBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCourseToggleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val course = items[position]
        holder.b.checkbox.setOnCheckedChangeListener(null)
        holder.b.checkbox.text = "${course.name} (${course.pars.size}홀)"
        holder.b.checkbox.isChecked = SelectionHolder.chosenCourseIds.contains(course.id)
        holder.b.checkbox.setOnCheckedChangeListener { _, checked ->
            SelectionHolder.chosenCourseIds.remove(course.id)
            if (checked) SelectionHolder.chosenCourseIds.add(course.id)
            onToggle()
        }
    }
    fun totalHoles(): Int =
        SelectionHolder.chosenCourseIds.sumOf { id -> items.firstOrNull { it.id == id }?.pars?.size ?: 0 }
}
