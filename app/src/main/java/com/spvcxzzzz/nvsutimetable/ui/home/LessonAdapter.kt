package com.spvcxzzzz.nvsutimetable.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.model.Lesson

class LessonAdapter(private val lessons: List<Lesson>) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        // Создаем view для элемента списка (item_week.xml)
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_week, parent, false)
        return LessonViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]

        // Заполняем данные в item_week.xml
        holder.disciplineTextView.text = lesson.DISCIPLINE
        holder.teacherTextView.text = lesson.TEACHER
        holder.dateTextView.text = lesson.DATEZAN
        holder.timeTextView.text = "${lesson.TIMEZAN} - ${lesson.TIMEZAN_END}"
    }

    override fun getItemCount(): Int {
        return lessons.size
    }

    // ViewHolder для элементов списка
    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val disciplineTextView: TextView = itemView.findViewById(R.id.tv_discipline)
        val teacherTextView: TextView = itemView.findViewById(R.id.tv_teacher)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_time)
    }
}
