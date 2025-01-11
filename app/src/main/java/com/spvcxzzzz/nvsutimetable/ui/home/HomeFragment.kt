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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.spvcxzzzz.nvsutimetable.model.DaySchedule
import com.spvcxzzzz.nvsutimetable.model.Lesson
import java.time.format.DateTimeFormatter


class HomeFragment : Fragment() {

    // View binding
    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    // Переменные для хранения группы и даты
    private var selectedGroup: String? = null
    private var selectedDateForApi: String? = null
    private var selectedDateCalendar: Calendar? = null

    var isScrollingUp = false
    var isScrollingDown = false

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
            // Если ключ еще не существует
            val sharedPreferences1 = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("week_timetable_view", false)
                apply()
            }
        }

        if (sharedPreferences.getBoolean("week_timetable_view", false) == true) {
            binding.calendarView2.visibility = View.GONE
            binding.fabPickToday.shrink()
        } else {
            binding.calendarView2.visibility = View.VISIBLE
            binding.fabPickToday.extend()
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
                        changeWeek(2)
                    }
                    // Свайп влево (прибавить день)
                    else if (diffX < -threshold && Math.abs(velocityX) > velocityThreshold) {
                        changeWeek(1)
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
                } else {
                    binding.textViewDate.text = formattedDateForDisplay
                    //Проверка на показ в виде недели, если да, то надо изменить дату для функции запроса расписания на неделю:
                    if (isRequestDayOrWeek() == true) {
                        changeWeek(3) //любой ключ отличный от 1 и 2 - текущая неделя
                    } else {
                        sendJsonRequest()
                    }
                }

            }

        // Открытие диалога для ввода номера группы
        binding.group.setOnClickListener { showNumberInputDialog(binding.group) }

        binding.calendarView2.setOnDateChangeListener { view, year, month, dayOfMonth ->
            // Создаём объект Calendar
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            selectedDateCalendar = calendar

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

    //Next = 1 - следующая неделя, 2 - предыдущая, иначе - текущая
    private fun changeWeek(Next: Int) {
        if (Next == 1) {
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

            binding.textViewDate.setText(getFormattedDateRange(calendar))
            selectedDateForApi = newFormattedDate

            selectedDateCalendar = calendar.clone() as Calendar

            fetchLessons()
        } else if (Next == 2) {
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

            binding.textViewDate.setText(getFormattedDateRange(calendar))
            selectedDateForApi = newFormattedDate

            selectedDateCalendar = calendar.clone() as Calendar

            fetchLessons()
        } else {
            val selectedDateInMillis = System.currentTimeMillis() // Берем текущую реальную дату в миллисекундах

            // Получаем текущую дату и время в объект Calendar
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY // Устанавливаем понедельник как первый день недели
                timeInMillis = selectedDateInMillis
            }

            // Определяем день недели с учетом, что неделя начинается с понедельника
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Вычисляем смещение до понедельника текущей недели
            val daysToMonday = if (dayOfWeek == Calendar.MONDAY) {
                0 // Если сегодня понедельник, смещение не требуется
            } else {
                if (dayOfWeek < Calendar.MONDAY) {
                    // В случае если день недели — воскресенье
                    -6
                } else {
                    Calendar.MONDAY - dayOfWeek
                }
            }

            // Перемещаем календарь на понедельник текущей недели
            calendar.add(Calendar.DAY_OF_MONTH, daysToMonday)

            // Обновляем CalendarView на понедельник текущей недели
            binding.calendarView2.date = calendar.timeInMillis

            val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

            // Получаем новую дату и форматируем ее
            val newFormattedDate = dateFormat.format(calendar.time)

            binding.textViewDate.text = getFormattedDateRange(calendar)
            selectedDateForApi = newFormattedDate

            // Устанавливаем selectedDateCalendar как копию календаря
            selectedDateCalendar = calendar.clone() as Calendar

            fetchLessons()
        }
    }

    fun getFormattedDateRange(calendar: Calendar): String {
        // Получаем текущую дату (например, 6 января)
        val startDay = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("ru")) // Месяц на русском

        // Добавляем к дате неделю
        calendar.add(Calendar.DAY_OF_MONTH, 6) // Сдвигаем на 6 дней вперед, чтобы получить вторую дату
        val endDay = calendar.get(Calendar.DAY_OF_MONTH)

        val month2 = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("ru")) // Месяц на русском

        // Формируем строку
        return "$startDay $month - $endDay $month2"
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

    // Функция для плавного скрытия Recycler при загрузке
    fun showLoaderHideRecycler() {
        binding.recyclerView.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(100)
            .withEndAction {  // После завершения анимации скрываем recycler
                binding.recyclerView.visibility = View.GONE
            }

        binding.loaderLayout.visibility = View.VISIBLE  // Делаем Loader видимым
        binding.loaderLayout.alpha = 0f  // Устанавливаем начальную прозрачность
        binding.loaderLayout.animate()
            .alpha(1f)  // Увеличиваем прозрачность до 1
            .setDuration(200)
    }

    // Функция для плавного показа Recycler после загрузки
    fun hideLoaderShowRecycler() {
        binding.loaderLayout.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(200)
            .withEndAction {  // После завершения анимации скрываем recycler
                binding.loaderLayout.visibility = View.GONE
            }

        binding.recyclerView.visibility = View.VISIBLE  // Делаем Loader видимым
        binding.recyclerView.alpha = 0f  // Устанавливаем начальную прозрачность
        binding.recyclerView.animate()
            .alpha(1f)  // Увеличиваем прозрачность до 1
            .setDuration(200)
    }

    // Функция для плавного скрытия занятий нет и ресайкла при загрузке
    fun hideAllShowLoader() {
        binding.recyclerView.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(100)
            .withEndAction {  // После завершения анимации скрываем recycler
                binding.recyclerView.visibility = View.GONE
            }

        binding.noDisciplinesLayout.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(100)
            .withEndAction {  // После завершения анимации скрываем noDisciplinesLayout
                binding.recyclerView.visibility = View.GONE
            }

        binding.loaderLayout.visibility = View.VISIBLE  // Делаем Loader видимым
        binding.loaderLayout.alpha = 0f  // Устанавливаем начальную прозрачность
        binding.loaderLayout.animate()
            .alpha(1f)  // Увеличиваем прозрачность до 1
            .setDuration(200)
    }

    // Функция для плавного показа занятий нет после загрузки
    fun hideLoaderShowNotimetable() {
        binding.loaderLayout.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(200)
            .withEndAction {  // После завершения анимации скрываем recycler
                binding.loaderLayout.visibility = View.GONE
            }

        binding.recyclerView.animate()
            .alpha(0f)  // Уменьшаем прозрачность до 0
            .setDuration(300)
            .withEndAction {  // После завершения анимации скрываем RecyclerView
                binding.recyclerView.visibility = View.GONE
            }

        binding.noDisciplinesLayout.visibility = View.VISIBLE  // Делаем Loader видимым
        binding.noDisciplinesLayout.alpha = 0f  // Устанавливаем начальную прозрачность
        binding.noDisciplinesLayout.animate()
            .alpha(1f)  // Увеличиваем прозрачность до 1
            .setDuration(200)
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

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val client = OkHttpClient()

                // Создаем запросы для каждой даты
                val request = Request.Builder()
                    .url("http://timetable.nvsuedu.ru/tm/index.php/json?&group=$group&date=$date")
                    .build()
                // Параллельное выполнение запросов
                val responseDeferred = async(Dispatchers.IO) { client.newCall(request).execute() }

                hideAllShowLoader()

                // Получение ответов
                val response = responseDeferred.await()

                val jsonResponse = response.body?.string()

                val timetableList = parseTimetableResponse(jsonResponse)

                val isEmpty = timetableList.isEmpty()
                // Передаем флаг в адаптер
                val adapter = TimetableAdapter(timetableList, isEmpty)
                binding.recyclerView.adapter = adapter

                displayTimetable(timetableList)
                if (isEmpty == true) {
                    hideLoaderShowNotimetable()
                } else {
                    hideLoaderShowRecycler()
                }

            } catch (e: Exception) {
                // Обработка ошибок
                Toast.makeText(requireContext(), "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("NetworkError", e.localizedMessage ?: "Unknown error")
            }
        }
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
            hideLoaderShowNotimetable() // Показываем layout "Занятий нет" и скрываем лоадер
            return emptyList()
        }

        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Timetable>>() {}.type
            val timetableList: List<Timetable> = gson.fromJson(jsonResponse, type) ?: emptyList()

            if (timetableList.isEmpty()) {
                hideLoaderShowNotimetable() // Показываем layout "Занятий нет" и скрываем лоадер
                return emptyList()
            } else {
                hideNoTimetableLayout()  // Скрываем layout "Занятий нет"
                return timetableList
            }
        } catch (e: JsonSyntaxException) {
            Toast.makeText(requireContext(), "Ошибка при парсинге данных: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            showNoTimetableLayout()  // Показываем layout "Занятий нет" при ошибке
            return emptyList()
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

    private fun getNextDate(date: String, daysToAdd: Int): String {
        val sdf = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(date)
        calendar.add(Calendar.DATE, daysToAdd)
        return sdf.format(calendar.time)
    }

    private fun displayTimetable(timetableList: List<Timetable>) {
        // Проверяем, пустой ли список
        val isEmpty = timetableList.isEmpty()
        // Передаем флаг в адаптер
        val adapter = TimetableAdapter(timetableList, isEmpty)
        binding.recyclerView.adapter = adapter

    }

    fun isRequestDayOrWeek(): Boolean {
        // Получаем SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Получаем значение по ключу "week_timetable_view" (по умолчанию false)
        val isWeekViewEnabled = sharedPreferences.getBoolean("week_timetable_view", false)

        // В зависимости от значения вызываем нужную функцию
        if (isWeekViewEnabled) {
            return true// Если week_timetable_view = true
        } else {
            return false // Если week_timetable_view = false
        }
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
        val group = selectedGroup ?: run {
            Toast.makeText(requireContext(), "Введите номер группы", Toast.LENGTH_SHORT).show()
            return
        } // Или получаем из вашего источника
        val selectedDate = selectedDateCalendar // Или любую другую дату, которую вы выбрали
        val weekDates = getWeekDates(selectedDate!!) // Это возвращает List<String>

        // Задаем значения для переменных
        val date1 = weekDates[0]
        val date2 = weekDates[1]
        val date3 = weekDates[2]
        val date4 = weekDates[3]
        val date5 = weekDates[4]
        val date6 = weekDates[5]

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val client = OkHttpClient()

                // Создаем запросы для каждой даты
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

                // Параллельное выполнение запросов
                val response1Deferred = async(Dispatchers.IO) { client.newCall(request1).execute() }
                val response2Deferred = async(Dispatchers.IO) { client.newCall(request2).execute() }
                val response3Deferred = async(Dispatchers.IO) { client.newCall(request3).execute() }
                val response4Deferred = async(Dispatchers.IO) { client.newCall(request4).execute() }
                val response5Deferred = async(Dispatchers.IO) { client.newCall(request5).execute() }
                val response6Deferred = async(Dispatchers.IO) { client.newCall(request6).execute() }

                showLoaderHideRecycler()

                // Получение ответов
                val response1 = response1Deferred.await()
                val response2 = response2Deferred.await()
                val response3 = response3Deferred.await()
                val response4 = response4Deferred.await()
                val response5 = response5Deferred.await()
                val response6 = response6Deferred.await()

                hideLoaderShowRecycler()

                // Преобразуем JSON в списки уроков
                val lessonsType = object : TypeToken<List<Lesson>>() {}.type
                val lessons1: List<Lesson> = Gson().fromJson(response1.body?.string(), lessonsType)
                val lessons2: List<Lesson> = Gson().fromJson(response2.body?.string(), lessonsType)
                val lessons3: List<Lesson> = Gson().fromJson(response3.body?.string(), lessonsType)
                val lessons4: List<Lesson> = Gson().fromJson(response4.body?.string(), lessonsType)
                val lessons5: List<Lesson> = Gson().fromJson(response5.body?.string(), lessonsType)
                val lessons6: List<Lesson> = Gson().fromJson(response6.body?.string(), lessonsType)

                val date1ForView = dateFromApiToViewInNameOfDay(date1)
                val date2ForView = dateFromApiToViewInNameOfDay(date2)
                val date3ForView = dateFromApiToViewInNameOfDay(date3)
                val date4ForView = dateFromApiToViewInNameOfDay(date4)
                val date5ForView = dateFromApiToViewInNameOfDay(date5)
                val date6ForView = dateFromApiToViewInNameOfDay(date6)

                // Список дней недели
                val weekDays = listOf("Понедельник, $date1ForView", "Вторник, $date2ForView", "Среда, $date3ForView", "Четверг, $date4ForView", "Пятница, $date5ForView", "Суббота, $date6ForView")

                // Формируем расписание для каждого дня
                val daySchedules = mutableListOf<DaySchedule>()

                for (i in weekDays.indices) {
                    val lessonsForDay = when (i) {
                        0 -> lessons1
                        1 -> lessons2
                        2 -> lessons3
                        3 -> lessons4
                        4 -> lessons5
                        5 -> lessons6
                        else -> emptyList()
                    }

                    val daySchedule = DaySchedule(weekDays[i], lessonsForDay)
                    daySchedules.add(daySchedule)
                }

                // Устанавливаем адаптер для RecyclerView
                val adapter = WeeklyScheduleAdapter(daySchedules)
                binding.recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                // Обработка ошибок
                Toast.makeText(requireContext(), "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Функция для того чтобы из dd_MM_yyyy String сделать d MMMM
    fun dateFromApiToViewInNameOfDay(inputDate: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy", Locale.getDefault())
        val outputFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

        val date = LocalDate.parse(inputDate, inputFormatter)
        val formattedDate = date.format(outputFormatter)
        return if (formattedDate != null) {
            formattedDate
        } else {
            inputDate
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
