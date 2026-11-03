package com.example.ascend.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ascend.data.ScheduleData
import com.example.ascend.model.ScheduleItem

@Composable
fun MainScreen() {

    val schedule = ScheduleData.weekdaySchedule

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {
            Text(
                text = "Today's Schedule",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        items(schedule) { item ->
            ScheduleCard(item)
        }
    }
}

@Composable
fun ScheduleCard(item: ScheduleItem) {

    var checked by remember {
        mutableStateOf(item.completed)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text(text = item.title)
            Text(text = "${item.startTime} - ${item.endTime}")
        }

        Checkbox(
            checked = checked,
            onCheckedChange = {
                checked = it
            }
        )
    }
}