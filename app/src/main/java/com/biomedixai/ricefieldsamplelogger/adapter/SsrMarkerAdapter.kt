package com.biomedixai.ricefieldsamplelogger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.biomedixai.ricefieldsamplelogger.R
import com.biomedixai.ricefieldsamplelogger.biokt.rice.SsrMarkerResult

class SsrMarkerAdapter(
    private var items: List<SsrMarkerResult> = emptyList()
) : RecyclerView.Adapter<SsrMarkerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSsrMotif: TextView = view.findViewById(R.id.tvSsrMotif)
        val tvSsrRange: TextView = view.findViewById(R.id.tvSsrRange)
        val tvSsrRepeats: TextView = view.findViewById(R.id.tvSsrRepeats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ssr_marker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvSsrMotif.text = item.motif
        holder.tvSsrRange.text = "Range: ${item.startIndex} - ${item.endIndex} bp"
        holder.tvSsrRepeats.text = "${item.repeatCount} Repeats"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<SsrMarkerResult>) {
        items = newItems
        notifyDataSetChanged()
    }
}
