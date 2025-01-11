package com.spvcxzzzz.nvsutimetable.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.model.DaySchedule

class WeeklyScheduleAdapter(
    private val weekSchedules: List<DaySchedule>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_LESSONS = 1
    private val VIEW_TYPE_NO_LESSONS = 2

    override fun getItemViewType(position: Int): Int {
        return if (weekSchedules[position].lessons.isEmpty()) VIEW_TYPE_NO_LESSONS else VIEW_TYPE_LESSONS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LESSONS) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_day_schedule, parent, false)
            DayScheduleViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_no_lessons, parent, false)
            NoLessonsViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val daySchedule = weekSchedules[position]

        if (holder is DayScheduleViewHolder) {
            holder.bind(daySchedule)
        } else if (holder is NoLessonsViewHolder) {
            holder.bind(daySchedule.dayOfWeek)
        }
    }

    override fun getItemCount(): Int {
        return weekSchedules.size
    }

    // ViewHolder для расписания с занятиями
    inner class DayScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayTextView: TextView = itemView.findViewById(R.id.tv_day)
        private val lessonsRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_lessons)

        init {
            // Настроим LayoutManager для RecyclerView с уроками
            lessonsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        }

        fun bind(daySchedule: DaySchedule) {
            dayTextView.text = daySchedule.dayOfWeek

            // Если список занятий пустой, можно скрыть RecyclerView
            if (daySchedule.lessons.isNullOrEmpty()) {
                lessonsRecyclerView.visibility = View.GONE
            } else {
                lessonsRecyclerView.visibility = View.VISIBLE
                // Убедитесь, что адаптер создается только один раз
                val adapter = LessonAdapter(daySchedule.lessons)
                lessonsRecyclerView.adapter = adapter
            }
        }
    }


    // ViewHolder для пустого дня
    inner class NoLessonsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayTextView: TextView = itemView.findViewById(R.id.tv_day_no_lessons)

        fun bind(dayOfWeek: String) {
            dayTextView.text = dayOfWeek
        }
    }
}
