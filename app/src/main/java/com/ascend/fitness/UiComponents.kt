package com.ascend.fitness

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.launch
import java.time.LocalDate


@Composable fun Header(title:String, subtitle:String) {
    Column {
        Text("ASCEND", color=Green, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
        Text(title, color=Ink, fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text(subtitle, color=Muted)
    }
}
@Composable fun Section(title:String, trailing:String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color=Ink, fontSize=18.sp, fontWeight=FontWeight.Bold)
        Text(trailing, color=Muted, fontSize=12.sp)
    }
}
@Composable fun Macro(label:String,value:String) {
    Column { Text(label,color=Color.White.copy(alpha=.8f),fontSize=11.sp); Text(value,color=Color.White,fontWeight=FontWeight.Bold) }
}
@Composable fun MealCard(meal:Meal, checked:Boolean, onCheck:()->Unit) {
    Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Mint,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center) { Text(meal.emoji,fontSize=26.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(meal.time,color=Muted,fontSize=11.sp)
                Text(meal.title,color=Ink,fontWeight=FontWeight.SemiBold)
                Text(meal.ingredients,color=Muted,fontSize=12.sp)
                Text("${meal.kcal} kcal · P ${meal.protein}g · C ${meal.carbs}g · F ${meal.fat}g",color=Green,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            IconButton(onClick=onCheck) { Icon(if(checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,"Mark complete",tint=if(checked) Green else Muted) }
        }
    }
}
