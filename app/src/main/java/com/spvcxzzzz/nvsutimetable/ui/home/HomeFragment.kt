package com.spvcxzzzz.nvsutimetable.ui.home

import android.annotation.SuppressLint
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
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.updatePadding
import com.google.android.material.search.SearchBar


class HomeFragment : Fragment() {

    // View binding
    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    // Переменные для хранения группы и даты
    private var selectedGroup: String? = null
    private var selectedDateForApi: String? = null

    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
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
        selectedGroup?.let { binding.group.hint = it }
        if (sharedPreferences.getString("group_number", null) != null) {
            binding.group.updatePadding(right = 20.dpToPx())
        }

        var isWeekTimetableView = sharedPreferences.getBoolean("week_timetable_view", false)

        if (!sharedPreferences.contains("week_timetable_view")) {
            // Если ключ ещ не существует
            val sharedPreferences1 = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("week_timetable_view", false)
                apply()
            }
        }

        SendRequestDayOrWeek()

        // Инициализация обработчиков кликов
        initClickListeners()

        // Настройка RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Устанавливаем обработчики для FAB
//        binding.fabGroup.setOnClickListener {
//            // Вызов функции для выбора группы
//            showNumberInputDialog(binding.group)
//        }

//        binding.fabToday.setOnClickListener {
//            // Вызов функции выбора даты
//            showDatePicker { selectedDate, formattedDateForApi ->
//                binding.textViewDate.text = selectedDate
//                selectedDateForApi = formattedDateForApi
//            }
//        }

        // Инициализация GestureDetector после view привязаки
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val threshold = 300 // минимальное расстояние для свайпа
                val velocityThreshold = 100 // минимальная скорость для свайпа

                if (e1 == null || e2 == null) return false

                // Вычисляем разницу по горизонтали
                val diffX = e2.x - e1.x

                // Свайп вправо (убавить день)
                if (diffX > threshold && Math.abs(velocityX) > velocityThreshold) {
                    changeDate(-1) // Уменьшаем дату
                }
                // Свайп влево (прибавить день)
                else if (diffX < -threshold && Math.abs(velocityX) > velocityThreshold) {
                    changeDate(1) // Прибавляем дату
                }

                return true // Возвращаем true для обработки свайпа
            }
        })

        binding.constraintLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        return binding.root
    }

    fun Int.dpToPx(): Int {
        return (this * binding.root.resources.displayMetrics.density).toInt()
    }

    private fun initClickListeners() {
            binding.fabPickToday.setOnClickListener {
                val todayMillis = Calendar.getInstance().timeInMillis
                binding.calendarView2.setDate(todayMillis, true, true)

                val calendar = Calendar.getInstance()

                // Форматируем дату для отображения и API
                val formattedDateForDisplay = formatDateForDisplay(calendar.time)
                val formattedDateForApi = formatDateForApi(calendar.time)

                selectedDateForApi = formattedDateForApi
                if (binding.textViewDate.text == formattedDateForDisplay) {
//                    Toast.makeText(requireContext(), "без запроса", Toast.LENGTH_SHORT).show()
                } else {
                    binding.textViewDate.text = formattedDateForDisplay
                    SendRequestDayOrWeek()
                }

            }

        // Открытие DatePicker для выбора даты
//        binding.fabToday.setOnClickListener {
//            showDatePicker { selectedDate, formattedDateForApi ->
//                binding.textViewDate.text = selectedDate
//                selectedDateForApi = formattedDateForApi
//            }
//        }

        // Открытие диалога для ввода номера группы
        binding.group.setOnClickListener { showNumberInputDialog(binding.group) }

        binding.calendarView2.setOnDateChangeListener { view, year, month, dayOfMonth ->
            // Создаём объект Calendar
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            // Получаем количество миллисекунд, чтобы использовать как в ModalDatePicker
            val selectedDateMillis = calendar.timeInMillis

            // Форматируем дату для отображения и API
            val formattedDateForDisplay = formatDateForDisplay(calendar.time)
            val formattedDateForApi = formatDateForApi(calendar.time)

            selectedDateForApi = formattedDateForApi
            if (binding.textViewDate.text == formattedDateForDisplay) {
//                Toast.makeText(requireContext(), "без запроса", Toast.LENGTH_SHORT).show()
            } else {
                binding.textViewDate.text = formattedDateForDisplay
                SendRequestDayOrWeek()
            }
        }
    }

    private fun changeDate(dayOffset: Int) {
        // Форматируем строку в объект Date
        val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val date = dateFormat.parse(selectedDateForApi)

        // Создаем объект Calendar и устанавливаем его на дату
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Прибавляем один день
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)

        // Получаем новую дату после прибавления одного дня
        val newDate = calendar.time

        // Форматируем новую дату обратно в строку
        val newFormattedDate = dateFormat.format(newDate)

        binding.textViewDate.setText(formatDateForDisplay(newDate))
        selectedDateForApi = newFormattedDate

        val timeInMillis: Long = newDate.time // Преобразуем Date в long

        binding.calendarView2.date = timeInMillis

        SendRequestDayOrWeek()
    }

    //Модал DatePicker, неактуален
//    private fun showDatePicker(onDateSelected: (String, String) -> Unit) {
//        // Преобразуем выбранную дату в миллисекунды, если она существует
//        val initialSelection = selectedDateForApi?.let {
//            // Преобразуем строку "dd_MM_yyyy" в объект Date
//            val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
//            // Устанавливаем временную зону UTC
//            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
//            val date = dateFormat.parse(it)
//
//            // Если дата корректно парсится, возвращаем её в миллисекундах, иначе выбираем сегодняшнюю дату
//            date?.time ?: MaterialDatePicker.todayInUtcMilliseconds()
//        } ?: MaterialDatePicker.todayInUtcMilliseconds()  // Если дата не выбрана, выбираем сегодняшнюю
//
//        val datePicker = MaterialDatePicker.Builder.datePicker()
//            .setSelection(initialSelection)  // Устанавливаем начальную выбранную дату
//            .setTitleText("Выберите дату")
//            .build()
//
//        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
//            val calendarSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
//            val formattedDateForDisplay = formatDateForDisplay(calendarSelected.time)
//            val formattedDateForApi = formatDateForApi(calendarSelected.time)
//            onDateSelected(formattedDateForDisplay, formattedDateForApi)
//            sendJsonRequest()
//        }
//
//        datePicker.show(childFragmentManager, datePicker.toString())
//    }


    private fun showNumberInputDialog(targetTextView: SearchBar) {
        val dialogView = layoutInflater.inflate(R.layout.input_group, null)
        val numberInput = dialogView.findViewById<TextView>(R.id.numberInput)

//        numberInput.text = selectedGroup

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Перейти") { _, _ ->
                val input = numberInput.text.toString()
                if (input.isNotEmpty()) {
                    targetTextView.hint = input
                    selectedGroup = input
                    saveGroupNumberToPrefs(input)
                    SendRequestDayOrWeek()
                    binding.group.updatePadding(right = 20.dpToPx())
                } else {
                    Toast.makeText(requireContext(), "Вы не ввели номер группы", Toast.LENGTH_SHORT).show()
                }
            }
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
            return listOf(Timetable("Занятий нет", "", "", "", "", "", "", "", ""))
        }

        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Timetable>>() {}.type
            val timetableList: List<Timetable> = gson.fromJson(jsonResponse, type) ?: emptyList()

            if (timetableList.isEmpty()) {
                showNoTimetableLayout()  // Показываем layout "Занятий нет"
                listOf(Timetable("", "", null, null, "", "", "", "", ""))
            } else {
                hideNoTimetableLayout()  // Скрываем layout "Занятий нет"
                timetableList
            }
        } catch (e: JsonSyntaxException) {
            Toast.makeText(requireContext(), "Ошибка при парсинге данных: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            showNoTimetableLayout()  // Показываем layout "Занятий нет" при ошибке
            listOf(Timetable("Занятий нет", "", null, null, "", "", "", "", ""))
        }
    }

    //Вычисление даты понедельника если чекнут расписание на неделю
    private fun getMondayOfWeek(selectedDate: String): String {
        val sdf = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val date = sdf.parse(selectedDate)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)  // Устанавливаем на понедельник
        return sdf.format(calendar.time)
    }

    //Создание реквеста если чекнут расписание на неделю
    private fun sendWeekJsonRequest() {
        val group = selectedGroup ?: run {
            Toast.makeText(requireContext(), "Введите номер группы", Toast.LENGTH_SHORT).show()
            return
        }

        val date = selectedDateForApi ?: run {
            Toast.makeText(requireContext(), "Выберите дату", Toast.LENGTH_SHORT).show()
            return
        }

        // Вычисляем понедельник для выбранной недели
        val mondayDate = getMondayOfWeek(date)
        val client = OkHttpClient()

        val timetableList = mutableListOf<Timetable>()
        val daysOfWeek = arrayOf(mondayDate, getNextDate(mondayDate, 1), getNextDate(mondayDate, 2), getNextDate(mondayDate, 3), getNextDate(mondayDate, 4), getNextDate(mondayDate, 5), getNextDate(mondayDate, 6))

        // Отправляем запросы для каждого дня недели
        for (day in daysOfWeek) {
            val url = "http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$day"
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        activity?.runOnUiThread {
                            val timetableForDay = parseTimetableResponse(jsonResponse)
                            timetableList.addAll(timetableForDay) // Добавляем данные за день в общий список
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
                    }
                }
            })
        }
    }

    private fun getNextDate(date: String, daysToAdd: Int): String {
        val sdf = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(date)
        calendar.add(Calendar.DATE, daysToAdd)
        return sdf.format(calendar.time)
    }

    private fun displayTimetable(timetableList: List<Timetable>) {
        Log.d("TimetableAdapter", "Передаем в адаптер список с размером: ${timetableList.size}")

        // Проверяем, пустой ли список
        val isEmpty = timetableList.isEmpty()

        // Передаем флаг в адаптер
        val adapter = TimetableAdapter(timetableList, isEmpty)
        binding.recyclerView.adapter = adapter
    }

    fun SendRequestDayOrWeek() {
        // Получаем SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Получаем значение по ключу "week_timetable_view" (по умолчанию false)
        val isWeekViewEnabled = sharedPreferences.getBoolean("week_timetable_view", false)

        // В зависимости от значения вызываем нужную функцию
        if (isWeekViewEnabled) {
            sendWeekJsonRequest() // Если week_timetable_view = true
        } else {
            sendJsonRequest() // Если week_timetable_view = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
