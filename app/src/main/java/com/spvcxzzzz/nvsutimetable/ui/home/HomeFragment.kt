package com.spvcxzzzz.nvsutimetable.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.databinding.FragmentTimetableBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    // Переменная для хранения значения группы (номер группы)
    private var groupNumber: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)

        // Установка текущей даты
        binding.textViewDate.text = getCurrentFormattedDate()

        // Загрузка сохраненного номера группы
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        groupNumber = sharedPreferences.getString("group_number", null)
        groupNumber?.let { binding.group.text = it }

        // Обработчик выбора даты
        binding.btnSelectDate.setOnClickListener {
            showDatePicker { selectedDate ->
                binding.textViewDate.text = selectedDate
            }
        }

        // Обработчик клика по номеру группы
        binding.group.setOnClickListener { showNumberInputDialog(binding.group) }

        return binding.root
    }

    // Диалог для выбора даты через MaterialDatePicker
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setTitleText("Выберите дату")
            .build()

        // Обработчик выбора даты
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendarSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }
            onDateSelected(formatDate(calendarSelected))
        }

        datePicker.show(childFragmentManager, datePicker.toString())
    }


    // Диалог для ввода/изменения номера группы
    private fun showNumberInputDialog(targetTextView: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.input_group, null)
        val numberInput = dialogView.findViewById<TextView>(R.id.numberInput)

        numberInput.text = groupNumber

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Введите номер группы")
            .setView(dialogView)
            .setPositiveButton("Перейти") { _, _ ->
                val input = numberInput.text.toString()
                if (input.isNotEmpty()) {
                    targetTextView.text = input
                    groupNumber = input
                    saveGroupNumberToPrefs(input)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Сохранение группы
    private fun saveGroupNumberToPrefs(groupNumber: String) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("group_number", groupNumber)
            apply()
        }
    }

    // Метод обновления TextView на текущую дату
    fun updateDateToToday() {
        binding.textViewDate.text = getCurrentFormattedDate()
    }

    // Получение текущей даты
    private fun getCurrentFormattedDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("E, d MMMM", Locale.getDefault()))
    }

    // Форматирование даты в виде строки
    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("E, d MMMM", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
