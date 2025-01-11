package com.spvcxzzzz.nvsutimetable.model

data class DaySchedule(
    val dayOfWeek: String,
    val lessons: List<Lesson>
)
