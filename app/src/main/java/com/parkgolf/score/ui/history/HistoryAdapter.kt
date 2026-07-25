package com.parkgolf.score.ui.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parkgolf.score.databinding.ItemHistoryRoundBinding
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val onClick: (Long) -> Unit) :
    RecyclerView.Adapter<HistoryAdapter.VH>() {
    private val items = mutableListOf<Round>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Round>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemHistoryRoundBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistoryRoundBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.b.tvDate.text = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(r.date))
        holder.b.tvVenue.text = r.venueName
        holder.b.tvTotal.text = "${Scoring.total(r.scores[0])}타"
        holder.b.root.setOnClickListener { onClick(r.id) }
    }
}
