package com.spvcxzzzz.nvsutimetable.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.model.Timetable

class TimetableAdapter(private val timetableList: List<Timetable>, private val isEmpty: Boolean) : RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder>() {

    inner class TimetableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val discipline: TextView = itemView.findViewById(R.id.textDiscipline)
        val type: TextView = itemView.findViewById(R.id.textType)
        val startOfLesson: TextView = itemView.findViewById(R.id.StartOfLession)
        val endOfLesson: TextView = itemView.findViewById(R.id.EndOfLession)
        val timeHolder: LinearLayout = itemView.findViewById(R.id.timeHolder)  // ссылка на LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timetable, parent, false)
        return TimetableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        if (isEmpty) {
            // Если список пуст, скрываем timeHolder
            holder.timeHolder.visibility = View.GONE
            holder.discipline.text = "Занятий нет"
            holder.type.visibility = View.GONE  // скрыть тип занятия, если нужно
        } else {
            val timetable = timetableList[position]
            holder.discipline.text = timetable.DISCIPLINE
            holder.type.text = timetable.VID

            // Обработка времени начала занятия
            val startTime = timetable.TIMEZAN?.takeIf { it.isNotEmpty() }?.substring(0, 5)
            if (startTime.isNullOrEmpty()) {
                holder.startOfLesson.visibility = View.GONE // скрываем, если нет данных
            } else {
                holder.startOfLesson.text = startTime
                holder.startOfLesson.visibility = View.VISIBLE // показываем, если есть данные
            }

            // Обработка времени окончания занятия
            val endTime = timetable.TIMEZAN_END?.takeIf { it.isNotEmpty() }?.substring(0, 5)
            if (endTime.isNullOrEmpty()) {
                holder.endOfLesson.visibility = View.GONE // скрываем, если нет данных
            } else {
                holder.endOfLesson.text = endTime
                holder.endOfLesson.visibility = View.VISIBLE // показываем, если есть данные
            }

            holder.timeHolder.visibility = View.VISIBLE  // показываем timeHolder
        }
    }

    override fun getItemCount(): Int {
        return timetableList.size
    }
}

