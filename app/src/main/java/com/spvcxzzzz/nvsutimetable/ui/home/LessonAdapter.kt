package com.spvcxzzzz.nvsutimetable.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.model.Lesson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class LessonAdapter(private val lessons: List<Lesson>) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        // Создаем view для элемента списка (item_week.xml)
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_week, parent, false)
        return LessonViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]

        println("Binding lesson: $lesson")

        fun distantCheck(): String {
            if (lesson.AUD == "Дистанционная") {
                val answer = "Дистанционная"
                return answer
            } else {
                val answer = lesson.AUD + " - " + lesson.Building + " корпус"
                return answer
            }
        }

        fun dateFromApiToView(inputDate: String): String {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            val outputFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))

            val date = LocalDate.parse(inputDate, inputFormatter)
            val formattedDate = date.format(outputFormatter)
            return if (formattedDate != null) {
                formattedDate
            } else {
                inputDate
            }
        }

        fun formatTeacherName(): String {
            // Декодируем Unicode строку
            val decodedName = lesson.TEACHER

            // Разбиваем строку на части
            val parts = decodedName.split(" ")

            // Если имя состоит из ФИО
            return if (parts.size >= 3) {
                "${parts[0]} ${parts[1].firstOrNull() ?: ""}.${parts[2].firstOrNull() ?: ""}."
            } else {
                decodedName // Если формат неправильный, возвращаем исходное имя
            }
        }

        fun typeParse(type: String): String {
            if (type == "Лб") {
                return "Лабораторная"
            } else if (type == "Лек") {
                return "Лекция"
            } else if (type == "КпЭ") {
                return "Консультация перед экзаменом"
            } else if (type == "Эк") {
                return "Экзамен"
            } else if (type == "!Практика") {
                return "Практика"
            } else return type
        }

        // Заполняем данные в item_week.xml
        holder.disciplineTextView.text = lesson.DISCIPLINE
        holder.teacherTextView.text = formatTeacherName()
        holder.timeTextView.text = "${lesson.TIMEZAN?.takeIf { it.isNotEmpty() }?.substring(0, 5)}"
        holder.timeEndTextView.text = "${lesson.TIMEZAN_END?.takeIf { it.isNotEmpty() }?.substring(0, 5)}"
        holder.audTextView.text = distantCheck()
        if (lesson.SUBGRUP != null)  {
            holder.subgroupTextView.text = lesson.GRUP + "/" + lesson.SUBGRUP
        } else {
            holder.subgroupTextView.visibility = View.GONE
        }
        holder.typeTextView.text = typeParse(lesson.VID)
    }

    override fun getItemCount(): Int {
        return lessons.size
    }

    // ViewHolder для элементов списка
    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val disciplineTextView: TextView = itemView.findViewById(R.id.tv_discipline)
        val teacherTextView: TextView = itemView.findViewById(R.id.tv_teacher)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_time)
        val timeEndTextView: TextView = itemView.findViewById(R.id.tv_time_end)
        val audTextView: TextView = itemView.findViewById(R.id.tv_aud)
        val subgroupTextView: TextView = itemView.findViewById(R.id.tv_subgroup)
        val typeTextView: TextView = itemView.findViewById(R.id.tv_type)
    }
}
