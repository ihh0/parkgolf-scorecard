package com.parkgolf.score.ui.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.parkgolf.score.R
import com.parkgolf.score.databinding.ItemHistoryRoundBinding
import com.parkgolf.score.domain.RecordFormat
import com.parkgolf.score.domain.Scoring
import com.parkgolf.score.domain.model.Round
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onView: (Long) -> Unit,
    private val onDelete: (Round) -> Unit,
) : RecyclerView.Adapter<HistoryAdapter.VH>() {
    private val items = mutableListOf<Round>()
    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<Round>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    inner class VH(val b: ItemHistoryRoundBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistoryRoundBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        val ctx = holder.b.root.context
        holder.b.tvDate.text = SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(r.date))
        holder.b.tvVenue.text = r.venueName

        val course = RecordFormat.courseName(r)
        holder.b.tvCourse.isVisible = course.isNotBlank()
        holder.b.tvCourse.text = course

        val total = Scoring.total(r.scores[0])
        val par = Scoring.parTotal(r.holes)
        val diff = total - par
        holder.b.tvTotal.text = total.toString()
        holder.b.tvPar.text = ctx.getString(R.string.grid_par_col) + par
        holder.b.tvBadge.text = RecordFormat.badgeText(diff)

        when {
            diff < 0 -> {
                holder.b.tvBadge.setBackgroundResource(R.drawable.bg_pill_primary)
                holder.b.tvBadge.setTextColor(MaterialColors.getColor(holder.b.tvBadge, R.attr.parkPrimary))
            }
            diff == 0 -> {
                holder.b.tvBadge.setBackgroundResource(R.drawable.bg_pill_muted)
                holder.b.tvBadge.setTextColor(MaterialColors.getColor(holder.b.tvBadge, R.attr.parkMutedForeground))
            }
            else -> {
                holder.b.tvBadge.setBackgroundResource(R.drawable.bg_pill_secondary)
                holder.b.tvBadge.setTextColor(MaterialColors.getColor(holder.b.tvBadge, R.attr.parkForeground))
            }
        }

        holder.b.tvPlayers.text = r.players.joinToString(", ")
        holder.b.btnViewResult.setOnClickListener { onView(r.id) }
        holder.b.btnDelete.setOnClickListener { onDelete(r) }
    }
}
