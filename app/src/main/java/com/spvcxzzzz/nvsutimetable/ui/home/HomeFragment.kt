package com.spvcxzzzz.nvsutimetable.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.databinding.FragmentTimetableBinding
import com.spvcxzzzz.nvsutimetable.model.Timetable
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import java.io.IOException

class HomeFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private var groupNumber: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)

        binding.textViewDate.text = getCurrentFormattedDate()

        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        groupNumber = sharedPreferences.getString("group_number", null)
        groupNumber?.let { binding.group.text = it }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker { selectedDate ->
                binding.textViewDate.text = selectedDate
            }
        }

        binding.group.setOnClickListener { showNumberInputDialog(binding.group) }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Обработчик кнопки для отправки запроса
        binding.btnSendJson.setOnClickListener {
            sendJsonRequest()
        }

        return binding.root
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setTitleText("Выберите дату")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val calendarSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }
            onDateSelected(formatDateForApi(calendarSelected))
        }

        datePicker.show(childFragmentManager, datePicker.toString())
    }

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

    private fun saveGroupNumberToPrefs(groupNumber: String) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("group_number", groupNumber)
            apply()
        }
    }

    private fun getCurrentFormattedDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("E, d MMMM", Locale.getDefault()))
    }

    private fun formatDateForApi(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun sendJsonRequest() {
        val group = binding.group.text.toString()
        val date = binding.textViewDate.text.toString()

        // Формирование URL для запроса
        val calendar = parseDate(date)
        val formattedDate = formatDateForApi(calendar)
        val url = "http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$formattedDate"

        // Создаем запрос с помощью OkHttp
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    activity?.runOnUiThread {
                        val timetableList = parseTimetableResponse(jsonResponse)
                        displayTimetable(timetableList)
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка при запросе", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    println(e.localizedMessage)
                }
            }
        })
    }

    private fun parseTimetableResponse(jsonResponse: String?): List<Timetable> {
        // Если ответ пустой или null, показываем "Занятий нет"
        if (jsonResponse.isNullOrEmpty()) {
            return listOf(Timetable("Занятий нет", "", "", "")) // Параметры TIMEZAN и TIMEZAN_END с пустыми значениями
        }

        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Timetable>>() {}.type
            val timetableList: List<Timetable> = gson.fromJson(jsonResponse, type) ?: emptyList()

            // Если список пуст, добавляем элемент с текстом "Занятий нет"
            if (timetableList.isEmpty()) {
                listOf(Timetable("Занятий нет", "", "", "")) // Параметры TIMEZAN и TIMEZAN_END с пустыми значениями
            } else {
                timetableList
            }
        } catch (e: JsonSyntaxException) {
            // Ошибка парсинга данных
            Toast.makeText(requireContext(), "Ошибка при парсинге данных: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            listOf(Timetable("Занятий нет", "", "", "")) // Вернем "Занятий нет" при ошибке парсинга
        }
    }

    private fun displayTimetable(timetableList: List<Timetable>) {
        Log.d("TimetableAdapter", "Передаем в адаптер список с размером: ${timetableList.size}")
        val adapter = TimetableAdapter(timetableList)
        binding.recyclerView.adapter = adapter
    }

    private fun parseDate(date: String): Calendar {
        val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val parsedDate = dateFormat.parse(date) ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = parsedDate
        return calendar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
