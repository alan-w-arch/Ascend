package com.example.ascend.data

import com.example.ascend.model.ScheduleItem

object ScheduleData {

    val weekdaySchedule = listOf(
        ScheduleItem(1, "Wake Up", "5:00 AM", "5:10 AM"),
        ScheduleItem(2, "Gym", "5:45 AM", "6:45 AM"),
        ScheduleItem(3, "Breakfast", "7:00 AM", "7:30 AM"),
        ScheduleItem(4, "Office", "9:00 AM", "5:00 PM"),
        ScheduleItem(5, "DSA", "6:15 PM", "7:15 PM"),
        ScheduleItem(6, "Backend / JS", "7:15 PM", "8:15 PM"),
        ScheduleItem(7, "Dinner", "8:15 PM", "9:00 PM"),
        ScheduleItem(8, "Learning Block", "9:00 PM", "10:15 PM"),
        ScheduleItem(9, "Sleep", "10:30 PM", "5:00 AM")
    )

    val sundaySchedule = listOf(
        ScheduleItem(1, "Wake Up", "6:00 AM", "6:15 AM"),
        ScheduleItem(2, "Projects", "10:00 AM", "1:00 PM"),
        ScheduleItem(3, "Relax / Movie", "6:00 PM", "9:00 PM")
    )
}