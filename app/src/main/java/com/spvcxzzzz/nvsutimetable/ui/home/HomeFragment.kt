package com.spvcxzzzz.nvsutimetable.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.databinding.FragmentTimetableBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentTimetableBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val view = inflater.inflate(R.layout.fragment_timetable, container, false)

        // Связываем фрагмент с разметкой
        val textViewDate: TextView = view.findViewById(R.id.textViewDate)
        val btnSelectDate: Button = view.findViewById(R.id.btnSelectDate)

        val formattedDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("E, d MMMM", Locale.getDefault()))
        textViewDate.text = formattedDate

        // Обработчик клика по кнопке
        btnSelectDate.setOnClickListener {
            showDatePicker { selectedDate ->
                textViewDate.text = selectedDate // Установить выбранную дату в TextView
            }
        }

        return view
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        // Текущая дата
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Создаем MaterialDatePicker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Текущая дата
            .setTitleText("Выберите дату")
            .build()



        // Обработчик выбора даты
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendarSelected = Calendar.getInstance()
            calendarSelected.timeInMillis = selectedDate
            val formattedDate = formatDate(calendarSelected)
            onDateSelected(formattedDate)
        }

        datePicker.show(childFragmentManager, datePicker.toString()) // Показываем MaterialDatePicker
    }

    // Функция для форматирования даты
    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("E, d MMMM", Locale.getDefault()) // Пример: Чт, 26 декабря
        return dateFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
