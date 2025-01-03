package com.spvcxzzzz.nvsutimetable.model

data class Timetable(
    val DISCIPLINE: String,
    val VID: String,
    val TIMEZAN: String? = null,
    val TIMEZAN_END: String? = null,
    val AUD: String,
    val Building: String,
    val TEACHER: String,
    val SUBGRUP: String,
    val GRUP: String
)
