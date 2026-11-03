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


@Composable fun Profile(p:PaddingValues,hostel:Boolean,tier:String,target:Int,proteinTarget:Int,waterTarget:Int,onHostel:(Boolean)->Unit,onTarget:(Int)->Unit,onProteinTarget:(Int)->Unit,onWaterTarget:(Int)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Your profile","Preferences for a plan that fits your life") }
        item {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs by context.ascendPreferences.data.collectAsState(initial=androidx.datastore.preferences.core.emptyPreferences())
            val today = LocalDate.now()
            val key = "weight_kg_$today"
            val savedWeight = prefs[ stringPreferencesKey(key) ] ?: ""
            var weightInput by remember(today) { mutableStateOf(savedWeight) }
            val validWeight = weightInput.toFloatOrNull()?.let { it in 20f..350f } == true
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Body weight",color=Ink,fontWeight=FontWeight.Bold,fontSize=18.sp)
                    Text("Optional daily check-in · kg",color=Muted,fontSize=12.sp)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value=weightInput,onValueChange={ weightInput=it.filter { c -> c.isDigit() || c=='.' }.take(6) },modifier=Modifier.weight(1f),label={Text("Weight (kg)")},singleLine=true,isError=weightInput.isNotBlank()&&!validWeight)
                        Button(enabled=validWeight,onClick={ scope.launch { context.ascendPreferences.edit { it[stringPreferencesKey(key)] = String.format(java.util.Locale.US,"%.1f",weightInput.toFloat()) } } }) { Text("Save") }
                    }
                    Text("Enter 20–350 kg. Weight entries stay on this device and are for personal tracking, not diagnosis.",color=Muted,fontSize=11.sp)
                    Divider()
                    Text("Last 7 days",color=Ink,fontWeight=FontWeight.SemiBold)
                    val weightEntries=(6 downTo 0).mapNotNull { offset ->
                        val day=today.minusDays(offset.toLong())
                        prefs[stringPreferencesKey("weight_kg_$day")]?.toFloatOrNull()?.let { day to it }
                    }
                    if(weightEntries.isNotEmpty()) {
                        val first=weightEntries.first().second
                        val latest=weightEntries.last().second
                        val delta=latest-first
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("Change across recorded days",color=Muted,fontSize=12.sp)
                            Text("${if(delta>0) "+" else ""}${String.format(java.util.Locale.US,"%.1f",delta)} kg",color=Ink,fontWeight=FontWeight.Bold,fontSize=12.sp)
                        }
                        Text("Range: ${String.format(java.util.Locale.US,"%.1f",weightEntries.minOf { it.second })}–${String.format(java.util.Locale.US,"%.1f",weightEntries.maxOf { it.second })} kg",color=Muted,fontSize=11.sp)
                    } else Text("Log a weight to start seeing your trend.",color=Muted,fontSize=12.sp)
                    (6 downTo 0).forEach { offset ->
                        val day=today.minusDays(offset.toLong())
                        val value=prefs[stringPreferencesKey("weight_kg_$day")]
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text(day.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")),color=Muted,fontSize=12.sp)
                            Text(if(value.isNullOrBlank()) "—" else "$value kg",color=if(value.isNullOrBlank()) Muted else Ink,fontWeight=FontWeight.Medium,fontSize=12.sp)
                        }
                    }
                    if(weightEntries.isNotEmpty()) {
                        OutlinedButton(
                            onClick={
                                val report=buildString {
                                    appendLine("Ascend body-weight log")
                                    appendLine("Date,Weight (kg)")
                                    weightEntries.forEach { (day,weight) -> appendLine("$day,${String.format(java.util.Locale.US,"%.1f",weight)}") }
                                }
                                val share=Intent(Intent.ACTION_SEND).apply {
                                    type="text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT,"Ascend body-weight log")
                                    putExtra(Intent.EXTRA_TEXT,report)
                                }
                                context.startActivity(Intent.createChooser(share,"Share weight log"))
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) { Icon(Icons.Default.Share,"Share weight log"); Spacer(Modifier.width(8.dp)); Text("Share 7-day weight log") }
                    }
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Personal details",color=Ink,fontWeight=FontWeight.Bold)
                    ProfileRow("Example goal","Healthy weight management")
                    ProfileRow("Daily energy target","%,d kcal".format(target))
                    ProfileRow("Recipe library","Vegetarian examples")
                    ProfileRow("Budget",when(tier) { "Budget" -> "Up to ₹50/meal"; "Standard" -> "Up to ₹75/meal"; else -> "Flexible" })
                    ProfileRow("Recipe budget filter",tier)
                    Text("Daily calorie target",color=Muted,fontSize=12.sp)
                    Slider(value=target.toFloat(),onValueChange={ onTarget(it.toInt()) },valueRange=1000f..4000f,steps=29)
                    Text("Adjust from 1,000 to 4,000 kcal in 100 kcal steps. This is a tracking target, not a personalized medical recommendation.",color=Muted,fontSize=11.sp)
                    Divider()
                    ProfileRow("Daily protein target","$proteinTarget g")
                    Slider(value=proteinTarget.toFloat(),onValueChange={onProteinTarget(it.toInt())},valueRange=30f..200f,steps=33)
                    Text("Set a personal tracking goal from 30 to 200 g/day. Protein needs vary; this setting is not medical advice.",color=Muted,fontSize=11.sp)
                    Divider()
                    ProfileRow("Daily water target","$waterTarget ml")
                    Slider(value=waterTarget.toFloat(),onValueChange={onWaterTarget((it.toInt()/250)*250)},valueRange=1000f..4000f,steps=11)
                    Text("Choose a tracking target from 1,000 to 4,000 ml in 250 ml steps. Fluid needs vary with activity, climate, and health.",color=Muted,fontSize=11.sp)
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Hostel mode",color=Ink,fontWeight=FontWeight.Bold); Text("Prioritize kettle/no-cook meals and simple equipment.",color=Muted,fontSize=12.sp) }
                    Switch(checked=hostel,onCheckedChange=onHostel)
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Ascend Plus",color=Ink,fontWeight=FontWeight.Bold)
                    Text("Planned: personalized plans, expanded recipes, and expert consultations.",color=Muted)
                    Button(onClick={},enabled=false,modifier=Modifier.fillMaxWidth()){Text("Membership coming soon · ₹99/month")}
                }
            }
        }
        item { Text("Hostel mode and recipe budget are saved on this device. Membership, expert booking, and professional plan purchases are not connected yet.",color=Muted,fontSize=11.sp) }
    }
}
@Composable fun ProfileRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,color=Muted); Text(value,color=Ink,fontWeight=FontWeight.Medium) }
}

