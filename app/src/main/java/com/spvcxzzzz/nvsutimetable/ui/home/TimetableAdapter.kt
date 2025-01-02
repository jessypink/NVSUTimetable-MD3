package com.spvcxzzzz.nvsutimetable.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.model.Timetable
import kotlinx.coroutines.delay

class TimetableAdapter(private val timetableList: List<Timetable>, private val isEmpty: Boolean) : RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder>() {

    inner class TimetableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val discipline: TextView = itemView.findViewById(R.id.textDiscipline)
        val type: TextView = itemView.findViewById(R.id.textType)
        val startOfLesson: TextView = itemView.findViewById(R.id.StartOfLession)
        val endOfLesson: TextView = itemView.findViewById(R.id.EndOfLession)
        val itemMainLayout: androidx.constraintlayout.widget.ConstraintLayout = itemView.findViewById(R.id.ItemLayout)
        val cabinet: TextView = itemView.findViewById(R.id.TextView_Cabinet)
        val prepod: TextView = itemView.findViewById(R.id.TextView_Lector)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timetable, parent, false)
        return TimetableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        if (isEmpty) {
            // Если список пуст, скрываем timeHolder
            holder.itemMainLayout.visibility = View.GONE
//            holder.timeHolder.visibility = View.GONE
//            holder.argsScrollView.visibility = View.GONE
//            holder.discipline.text = "Занятий нет"
//            holder.type.visibility = View.GONE  // скрыть тип занятия, если нужно
        } else {
            val timetable = timetableList[position]

            fun distantCheck() {
                if (timetable.AUD == "Дистанционная") {
                    holder.cabinet.text = "Дистанционная"
                } else {
                    holder.cabinet.text = timetable.AUD + " - " + timetable.Building + " корпус"
                }
            }


            holder.discipline.text = timetable.DISCIPLINE
            holder.type.text = timetable.VID
            distantCheck()
            fun formatTeacherName(): String {
                // Декодируем Unicode строку
                val decodedName = timetable.TEACHER

                // Разбиваем строку на части
                val parts = decodedName.split(" ")

                // Если имя состоит из ФИО
                return if (parts.size >= 3) {
                    "${parts[0]} ${parts[1].firstOrNull() ?: ""}.${parts[2].firstOrNull() ?: ""}."
                } else {
                    decodedName // Если формат неправильный, возвращаем исходное имя
                }
            }

            holder.prepod.text = formatTeacherName()

            // Обработка времени занятия
            val endTime = timetable.TIMEZAN_END?.takeIf { it.isNotEmpty() }?.substring(0, 5)
            val startTime = timetable.TIMEZAN?.takeIf { it.isNotEmpty() }?.substring(0, 5)
            if (startTime.isNullOrEmpty()) {
                holder.prepod.visibility = View.GONE
                holder.cabinet.visibility = View.GONE
                holder.startOfLesson.visibility = View.GONE
                holder.endOfLesson.visibility = View.GONE // скрываем, если нет данных
            } else {
                holder.startOfLesson.text = startTime
                holder.endOfLesson.text = endTime
//                holder.startOfLesson.visibility = View.VISIBLE
            }


            holder.itemMainLayout.visibility = View.VISIBLE
//            holder.timeHolder.visibility = View.VISIBLE
//            holder.argsScrollView.visibility = View.VISIBLE// показываем timeHolder
        }
    }

    override fun getItemCount(): Int {
        return timetableList.size
    }
}

