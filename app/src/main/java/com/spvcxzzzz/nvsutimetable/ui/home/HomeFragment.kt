package com.spvcxzzzz.nvsutimetable.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.spvcxzzzz.nvsutimetable.R
import com.spvcxzzzz.nvsutimetable.databinding.FragmentTimetableBinding
import com.spvcxzzzz.nvsutimetable.model.Timetable
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*


class HomeFragment : Fragment() {

    // View binding
    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    // Переменные для хранения группы и даты
    private var selectedGroup: String? = null
    private var selectedDateForApi: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация ViewModel
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)


        // Инициализация View Binding
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)

        // Установка текущей даты и её значения для API
        val currentDate = LocalDate.now()
        val currentDateAsDate = Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        binding.textViewDate.text = formatDateForDisplay(currentDateAsDate)
        selectedDateForApi = formatDateForApi(currentDateAsDate)

        // Загрузка номера группы из SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        selectedGroup = sharedPreferences.getString("group_number", null)
        selectedGroup?.let { binding.group.text = it }

        sendJsonRequest()

        // Инициализация обработчиков кликов
        initClickListeners()

        // Настройка RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Устанавливаем обработчики для FAB
        binding.fabGroup.setOnClickListener {
            // Вызов функции для выбора группы
            showNumberInputDialog(binding.group)
        }

        binding.fabToday.setOnClickListener {
            // Вызов функции выбора даты
            showDatePicker { selectedDate, formattedDateForApi ->
                binding.textViewDate.text = selectedDate
                selectedDateForApi = formattedDateForApi
            }
        }


        return binding.root
    }

    fun setFabGroupMargin(marginBottom: Float) {
        val layoutParams = binding.fabGroup.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = marginBottom.toInt()
        binding.fabGroup.layoutParams = layoutParams
    }


    private fun initClickListeners() {
        // Открытие DatePicker для выбора даты
        binding.fabToday.setOnClickListener {
            showDatePicker { selectedDate, formattedDateForApi ->
                binding.textViewDate.text = selectedDate
                selectedDateForApi = formattedDateForApi
            }
        }

        // Открытие диалога для ввода номера группы
        binding.group.setOnClickListener { showNumberInputDialog(binding.group) }


        // Отправка запроса на сервер
//        binding.btnSendJson.setOnClickListener {
//            sendJsonRequest()
//        }
    }

    private fun showDatePicker(onDateSelected: (String, String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setTitleText("Выберите дату")
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            val calendarSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val formattedDateForDisplay = formatDateForDisplay(calendarSelected.time)
            val formattedDateForApi = formatDateForApi(calendarSelected.time)
            onDateSelected(formattedDateForDisplay, formattedDateForApi)
            sendJsonRequest()
        }

            datePicker.show(childFragmentManager, datePicker.toString())
    }

    private fun showNumberInputDialog(targetTextView: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.input_group, null)
        val numberInput = dialogView.findViewById<TextView>(R.id.numberInput)

        numberInput.text = selectedGroup

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Введите номер группы")
            .setView(dialogView)
            .setPositiveButton("Перейти") { _, _ ->
                val input = numberInput.text.toString()
                if (input.isNotEmpty()) {
                    targetTextView.text = input
                    selectedGroup = input
                    saveGroupNumberToPrefs(input)
                    sendJsonRequest()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun formatDateForDisplay(date: Date): String {
        val dateFormat = SimpleDateFormat("E, d MMMM", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun formatDateForApi(date: Date): String {
        val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun getWeekDates(selectedDate: Calendar): List<String> {
        val dates = mutableListOf<String>()
        val calendar = selectedDate.clone() as Calendar

        // Переходим к началу недели (понедельник)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Добавляем дни недели, кроме воскресенья
        for (i in 0..5) {
            dates.add(formatDateForApi(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dates
    }

    private fun sendJsonRequest() {
        // Используем переменные для формирования URL
        val group = selectedGroup ?: run {
            Toast.makeText(requireContext(), "Введите номер группы", Toast.LENGTH_SHORT).show()
            return
        }

        val date = selectedDateForApi ?: run {
            Toast.makeText(requireContext(), "Выберите дату", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date"

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
                    Log.e("NetworkError", e.localizedMessage ?: "Unknown error")
                }
            }
        })
    }

    private fun saveGroupNumberToPrefs(groupNumber: String) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("group_number", groupNumber)
            apply()
        }
    }

    private fun parseTimetableResponse(jsonResponse: String?): List<Timetable> {

        val noTimetableLayout = binding.noDisciplinesLayout
        val lessonsRecyclerView = binding.recyclerView

        // Функция для плавного скрытия layout при отсутствии занятий
        fun hideNoTimetableLayout() {
            noTimetableLayout.animate()
                .alpha(0f)  // Уменьшаем прозрачность до 0
                .setDuration(200)
                .withEndAction {  // После завершения анимации скрываем layout
                    noTimetableLayout.visibility = View.GONE
                }

            lessonsRecyclerView.visibility = View.VISIBLE  // Делаем RecyclerView видимым
            lessonsRecyclerView.alpha = 0f  // Устанавливаем начальную прозрачность
            lessonsRecyclerView.animate()
                .alpha(1f)  // Увеличиваем прозрачность до 1
                .setDuration(200)
        }

        // Функция для плавного отображения layout если занятия есть
        fun showNoTimetableLayout() {
            noTimetableLayout.visibility = View.VISIBLE  // Делаем layout видимым
            noTimetableLayout.alpha = 0f  // Устанавливаем начальную прозрачность
            noTimetableLayout.animate()
                .alpha(1f)  // Увеличиваем прозрачность до 1
                .setDuration(300)

            lessonsRecyclerView.animate()
                .alpha(0f)  // Уменьшаем прозрачность до 0
                .setDuration(300)
                .withEndAction {  // После завершения анимации скрываем RecyclerView
                    lessonsRecyclerView.visibility = View.GONE
                }
        }

        if (jsonResponse.isNullOrEmpty()) {
            showNoTimetableLayout()  // Показываем layout "Занятий нет"
            return listOf(Timetable("Занятий нет", "", "", "", "", "", ""))
        }

        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Timetable>>() {}.type
            val timetableList: List<Timetable> = gson.fromJson(jsonResponse, type) ?: emptyList()

            if (timetableList.isEmpty()) {
                showNoTimetableLayout()  // Показываем layout "Занятий нет"
                listOf(Timetable("", "", null, null, "", "", ""))
            } else {
                hideNoTimetableLayout()  // Скрываем layout "Занятий нет"
                timetableList
            }
        } catch (e: JsonSyntaxException) {
            Toast.makeText(requireContext(), "Ошибка при парсинге данных: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            showNoTimetableLayout()  // Показываем layout "Занятий нет" при ошибке
            listOf(Timetable("Занятий нет", "", null, null, "", "", ""))
        }
    }

    private fun displayTimetable(timetableList: List<Timetable>) {
        Log.d("TimetableAdapter", "Передаем в адаптер список с размером: ${timetableList.size}")

        // Проверяем, пустой ли список
        val isEmpty = timetableList.isEmpty()

        // Передаем флаг в адаптер
        val adapter = TimetableAdapter(timetableList, isEmpty)
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}