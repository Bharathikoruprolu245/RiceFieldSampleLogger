package com.biomedixai.ricefieldsamplelogger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.biomedixai.ricefieldsamplelogger.R
import com.biomedixai.ricefieldsamplelogger.data.SampleEntity
import com.google.android.material.button.MaterialButton
import org.json.JSONArray

class SampleAdapter(
    private var items: List<SampleEntity> = emptyList(),
    private val onEditClick: (SampleEntity) -> Unit,
    private val onDeleteClick: (SampleEntity) -> Unit,
    private val onItemClick: (SampleEntity) -> Unit
) : RecyclerView.Adapter<SampleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSampleId: TextView = view.findViewById(R.id.tvSampleId)
        val tvVarietyBadge: TextView = view.findViewById(R.id.tvVarietyBadge)
        val tvDateLocation: TextView = view.findViewById(R.id.tvDateLocation)
        val tvWeatherSummary: TextView = view.findViewById(R.id.tvWeatherSummary)
        val tvGenomicSummary: TextView = view.findViewById(R.id.tvGenomicSummary)
        val btnEditSample: MaterialButton = view.findViewById(R.id.btnEditSample)
        val btnDeleteSample: MaterialButton = view.findViewById(R.id.btnDeleteSample)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sample, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvSampleId.text = item.sampleId
        holder.tvVarietyBadge.text = item.riceVariety
        holder.tvDateLocation.text = "📅 ${item.collectionDate} | 📍 ${String.format("%.4f", item.latitude)}, ${String.format("%.4f", item.longitude)}"
        holder.tvWeatherSummary.text = "🌡️ ${item.temperature}°C | 💧 ${item.humidity}% | 🌧️ ${item.precipitation}mm"

        val cisCount = try { JSONArray(item.cisElementsJson).length() } catch (e: Exception) { 0 }
        val ssrCount = try { JSONArray(item.ssrMarkersJson).length() } catch (e: Exception) { 0 }

        holder.tvGenomicSummary.text = "🧬 Seq: ${item.sequenceLength} bp | GC: ${String.format("%.1f", item.gcContent)}% | Cis: $cisCount | SSRs: $ssrCount"

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnEditSample.setOnClickListener { onEditClick(item) }
        holder.btnDeleteSample.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<SampleEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
