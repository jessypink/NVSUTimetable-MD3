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
import com.spvcxzzzz.nvsutimetable.model.Lesson


class HomeFragment : Fragment() {

    // View binding
    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    // Переменные для хранения группы и даты
    private var selectedGroup: String? = null
    private var selectedDateForApi: String? = null
    private var selectedDateCalendar: Calendar? = null

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

        val calendar: Calendar = Calendar.getInstance()
        selectedDateCalendar = calendar

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

                if (isWeekTimetableView == true) {
                    if (diffX > threshold && Math.abs(velocityX) > velocityThreshold) {
                        changeWeek(false)
                    }
                    // Свайп влево (прибавить день)
                    else if (diffX < -threshold && Math.abs(velocityX) > velocityThreshold) {
                        changeWeek(true)
                    }
                } else {
                    // Свайп вправо (убавить день)
                    if (diffX > threshold && Math.abs(velocityX) > velocityThreshold) {
                        changeDate(-1) // Уменьшаем дату
                    }
                    // Свайп влево (прибавить день)
                    else if (diffX < -threshold && Math.abs(velocityX) > velocityThreshold) {
                        changeDate(1) // Прибавляем дату
                    }
                }


                // Устанавливаем OnTouchListener на RecyclerView
                binding.recyclerView.setOnTouchListener { _, event ->
                    // Передаем событие в GestureDetector для обработки
                    gestureDetector.onTouchEvent(event)

                    // Возвращаем false, чтобы позволить RecyclerView обрабатывать остальные события касания (например, прокрутку)
                    return@setOnTouchListener false
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
            selectedDateCalendar = calendar

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

    private fun changeWeek(Next: Boolean) {
        if (Next == true) {
            // Получаем текущую выбранную дату из CalendarView
            val selectedDateInMillis = binding.calendarView2.date
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDateInMillis
            }

            // Определяем день недели
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Считаем, сколько дней до следующего понедельника
            val daysToNextMonday = if (dayOfWeek == Calendar.MONDAY) 7 else (Calendar.MONDAY - dayOfWeek + 7) % 7

            // Перемещаем дату на следующий понедельник
            calendar.add(Calendar.DAY_OF_MONTH, daysToNextMonday)

            // Обновляем CalendarView на следующую дату
            binding.calendarView2.date = calendar.timeInMillis

            val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

            // Получаем новую дату после прибавления одного дня
            val newDate = calendar.time

            // Форматируем новую дату обратно в строку
            val newFormattedDate = dateFormat.format(newDate)

            binding.textViewDate.setText(formatDateForDisplay(newDate))
            selectedDateForApi = newFormattedDate

            selectedDateCalendar = calendar.clone() as Calendar

            fetchLessons()
        } else {
            // Получаем текущую выбранную дату из CalendarView
            val selectedDateInMillis = binding.calendarView2.date
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDateInMillis
            }

            // Определяем день недели
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Вычисляем, сколько дней назад был понедельник прошлой недели
            val daysToPreviousMonday = if (dayOfWeek == Calendar.MONDAY) 7 else (dayOfWeek - Calendar.MONDAY + 7)

            // Перемещаем дату на понедельник прошлой недели
            calendar.add(Calendar.DAY_OF_MONTH, -daysToPreviousMonday)

            // Обновляем CalendarView на следующую дату
            binding.calendarView2.date = calendar.timeInMillis

            val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

            // Получаем новую дату после прибавления одного дня
            val newDate = calendar.time

            // Форматируем новую дату обратно в строку
            val newFormattedDate = dateFormat.format(newDate)

            binding.textViewDate.setText(formatDateForDisplay(newDate))
            selectedDateForApi = newFormattedDate

            selectedDateCalendar = calendar.clone() as Calendar

            fetchLessons()
        }
    }



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

    // Функция для плавного скрытия layout при отсутствии занятий
    fun hideNoTimetableLayout() {
        binding.noDisciplinesLayout.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(200)
            .withEndAction {  // После завершения анимации скрываем layout
                binding.noDisciplinesLayout.visibility = View.GONE
            }

        binding.recyclerView.visibility = View.VISIBLE  // Делаем RecyclerView видимым
        binding.recyclerView.alpha = 0f  // Устанавливаем начальную прозрачность
        binding.recyclerView.animate()
            .alpha(1f)  // Увеличиваем прозрачность до 1
            .setDuration(200)
    }

    // Функция для плавного отображения layout если занятия есть
    fun showNoTimetableLayout() {
        binding.noDisciplinesLayout.visibility = View.VISIBLE  // Делаем layout видимым
        binding.noDisciplinesLayout.alpha = 0f  // Устанавливаем начальную прозрачность
        binding.noDisciplinesLayout.animate()
            .alpha(1f)  // Увеличиваем прозрачность до 1
            .setDuration(300)

        binding.recyclerView.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(300)
            .withEndAction {  // После завершения анимации скрываем RecyclerView
                binding.recyclerView.visibility = View.GONE
            }
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
            fetchLessons() // Если week_timetable_view = true
        } else {
            sendJsonRequest() // Если week_timetable_view = false
        }
    }

    private fun fetchLessons() {
        // Получаем выбранную дату и группу
        val group = "3102" // Или получаем из вашего источника
        // Предположим, что getWeekDates возвращает список дат
        val selectedDate = selectedDateCalendar  // Или любую другую дату, которую вы выбрали
        val weekDates = getWeekDates(selectedDate!!)  // Это возвращает List<String>

        // Задаем значения для переменных
        val date1 = weekDates.get(0)
        val date2 = weekDates.get(1)
        val date3 = weekDates.get(2)
        val date4 = weekDates.get(3)
        val date5 = weekDates.get(4)
        val date6 = weekDates.get(5)

        println(date4 + " " + date5 + " " + date6 + " ")
        // Выполняем асинхронные запросы для двух дней
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val client = OkHttpClient()

                // Создаем запросы для двух дней
                val request1 = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date1")
                    .build()

                val request2 = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date2")
                    .build()

                val request3 = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date3")
                    .build()

                val request4 = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date4")
                    .build()

                val request5 = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date5")
                    .build()

                val request6 = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date6")
                    .build()

                // Отправляем запросы параллельно
                val response1Deferred = async(Dispatchers.IO) { client.newCall(request1).execute() }
                val response2Deferred = async(Dispatchers.IO) { client.newCall(request2).execute() }
                val response3Deferred = async(Dispatchers.IO) { client.newCall(request3).execute() }
                val response4Deferred = async(Dispatchers.IO) { client.newCall(request4).execute() }
                val response5Deferred = async(Dispatchers.IO) { client.newCall(request5).execute() }
                val response6Deferred = async(Dispatchers.IO) { client.newCall(request6).execute() }

                // Получаем ответы
                val response1 = response1Deferred.await()
                val response2 = response2Deferred.await()
                val response3 = response3Deferred.await()
                val response4 = response4Deferred.await()
                val response5 = response5Deferred.await()
                val response6 = response6Deferred.await()

                // Преобразуем JSON в список уроков с помощью Gson
                val lessonsType = object : TypeToken<List<Lesson>>() {}.type
                val lessons1: List<Lesson> = Gson().fromJson(response1.body?.string(), lessonsType)
                val lessons2: List<Lesson> = Gson().fromJson(response2.body?.string(), lessonsType)
                val lessons3: List<Lesson> = Gson().fromJson(response3.body?.string(), lessonsType)
                val lessons4: List<Lesson> = Gson().fromJson(response4.body?.string(), lessonsType)
                val lessons5: List<Lesson> = Gson().fromJson(response5.body?.string(), lessonsType)
                val lessons6: List<Lesson> = Gson().fromJson(response6.body?.string(), lessonsType)

                // Объединяем данные из запросов
                val allLessons = lessons1 + lessons2 + lessons3 + lessons4 + lessons5 + lessons6

                // Создаем и устанавливаем адаптер для RecyclerView
                val adapter = LessonAdapter(allLessons)
                binding.recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()

                // Проверяем, пусты ли все ответы
                val allResponsesEmpty = allLessons.isNullOrEmpty() // Проверка на пустоту всех элементов в списке

                // В зависимости от результата вызываем нужную функцию
                if (allResponsesEmpty) {
                    showNoTimetableLayout()  // Если все ответы пустые, скрываем расписание
                } else {
                    hideNoTimetableLayout()  // Если хотя бы один день не пуст, показываем расписание
                }

            } catch (e: Exception) {
                // Обработка ошибок
                Toast.makeText(requireContext(), "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
