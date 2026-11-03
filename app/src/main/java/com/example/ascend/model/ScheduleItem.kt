package com.example.ascend.model

data class ScheduleItem(
    val id: Int,
    val title: String,
    val startTime: String,
    val endTime: String,
    var completed: Boolean = false
)