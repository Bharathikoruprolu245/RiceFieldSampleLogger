package com.biomedixai.ricefieldsamplelogger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.biomedixai.ricefieldsamplelogger.R
import com.biomedixai.ricefieldsamplelogger.biokt.rice.CisElementResult

class CisElementAdapter(
    private var items: List<CisElementResult> = emptyList()
) : RecyclerView.Adapter<CisElementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCisName: TextView = view.findViewById(R.id.tvCisName)
        val tvCisPosition: TextView = view.findViewById(R.id.tvCisPosition)
        val tvCisMotif: TextView = view.findViewById(R.id.tvCisMotif)
        val tvCisDesc: TextView = view.findViewById(R.id.tvCisDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cis_element, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCisName.text = item.name
        holder.tvCisPosition.text = "Pos: ${item.position}"
        holder.tvCisMotif.text = "Motif: ${item.motif}"
        holder.tvCisDesc.text = item.description
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CisElementResult>) {
        items = newItems
        notifyDataSetChanged()
    }
}
