package com.spvcxzzzz.nvsutimetable.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.model.Timetable

class TimetableAdapter(private val timetableList: List<Timetable>) : RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder>() {

    inner class TimetableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val discipline: TextView = itemView.findViewById(R.id.textDiscipline)
        val type: TextView = itemView.findViewById(R.id.textType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timetable, parent, false)
        return TimetableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        val timetable = timetableList[position]
        Log.d("TimetableAdapter", "Binding item at position $position with discipline: ${timetable.DISCIPLINE}")
        holder.discipline.text = timetable.DISCIPLINE
        holder.type.text = timetable.VID
    }

    override fun getItemCount(): Int {
        return timetableList.size
    }
}

