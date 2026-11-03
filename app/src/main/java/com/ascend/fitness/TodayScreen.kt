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


private data class Meal(val name:String, val time:String, val title:String, val ingredients:String, val kcal:Int, val emoji:String, val protein:Int=0, val carbs:Int=0, val fat:Int=0)
private val meals = listOf(
    Meal("Breakfast","8:00 AM","Banana oats bowl","Oats · banana · milk",380,"🥣",14,62,9),
    Meal("Lunch","1:00 PM","Chickpea rice bowl","Chickpeas · rice · cucumber",520,"🍛",18,88,8),
    Meal("Snack","4:30 PM","Peanut banana bites","Banana · peanuts",180,"🍌",5,27,8),
    Meal("Dinner","8:00 PM","Moong dal khichdi","Moong dal · rice · spices",420,"🍲",17,72,7)
)
private fun sortMealsByTime(items: List<Meal>): List<Meal> = items.sortedBy { meal ->
    runCatching { java.time.LocalTime.parse(meal.time, java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a").toFormatter(java.util.Locale.US)).toSecondOfDay() }
        .getOrDefault(Int.MAX_VALUE)
}


@Composable fun Today(p:PaddingValues, target:Int, proteinTarget:Int, done:Set<String>, customMeals:List<CustomMeal>, onAdd:(CustomMeal)->Unit, onUpdate:(CustomMeal)->Unit, onDone:(String)->Unit, onRemove:(CustomMeal)->Unit) {
    var editingMeal by remember { mutableStateOf<CustomMeal?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val waterPreferences by context.ascendPreferences.data.collectAsState(initial=androidx.datastore.preferences.core.emptyPreferences())
    val waterKey = "water_ml_${LocalDate.now()}"
    val waterMl = waterPreferences[androidx.datastore.preferences.core.intPreferencesKey(waterKey)] ?: 0
    val waterTarget = waterPreferences[androidx.datastore.preferences.core.intPreferencesKey("water_target_ml")] ?: 2000
    val waterScope = rememberCoroutineScope()
    fun updateWater(delta: Int) {
        waterScope.launch { context.ascendPreferences.edit { prefs -> prefs[androidx.datastore.preferences.core.intPreferencesKey(waterKey)] = (waterMl + delta).coerceAtLeast(0) } }
    }
    val allMeals = sortMealsByTime(meals + customMeals.map { Meal("custom-${it.id}",it.time,it.title,it.ingredients,it.kcal,"🍽️",it.protein,it.carbs,it.fat) })
    val loggedMeals = allMeals.filter { it.name in done }
    val consumed = loggedMeals.sumOf { it.kcal }
    val protein = loggedMeals.sumOf { it.protein }
    val carbs = loggedMeals.sumOf { it.carbs }
    val fat = loggedMeals.sumOf { it.fat }
    // Daily target is provided by the saved profile preference.
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header(when (java.time.LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" },"Your plan for today") }
        item {
            val moodKey=androidx.datastore.preferences.core.intPreferencesKey("wellness_mood_${LocalDate.now()}")
            val energyKey=androidx.datastore.preferences.core.intPreferencesKey("wellness_energy_${LocalDate.now()}")
            val mood=waterPreferences[moodKey] ?: 0
            val energy=waterPreferences[energyKey] ?: 0
            val moodLabels=listOf("Very low","Low","Okay","Good","Great")
            val energyLabels=listOf("Drained","Low","Moderate","High","Energized")
            val weeklyHistory=(6 downTo 0).map { offset ->
                val day=LocalDate.now().minusDays(offset.toLong())
                val m=waterPreferences[androidx.datastore.preferences.core.intPreferencesKey("wellness_mood_$day")]
                val e=waterPreferences[androidx.datastore.preferences.core.intPreferencesKey("wellness_energy_$day")]
                Triple(day,m,e)
            }
            val weeklyCheckins=weeklyHistory.mapNotNull { (_,m,e) -> if(m!=null && e!=null) m to e else null }
            Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Daily wellbeing check-in",color=Ink,fontWeight=FontWeight.Bold,fontSize=18.sp)
                    Text("A quick optional snapshot of how you feel today.",color=Muted,fontSize=12.sp)
                    Text("Mood",color=Ink,fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        moodLabels.forEachIndexed { index,label ->
                            FilterChip(selected=mood==index+1,onClick={waterScope.launch { context.ascendPreferences.edit { it[moodKey]=index+1 } }},label={Text(label,fontSize=10.sp)},modifier=Modifier.weight(1f))
                        }
                    }
                    Text("Energy",color=Ink,fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        energyLabels.forEachIndexed { index,label ->
                            FilterChip(selected=energy==index+1,onClick={waterScope.launch { context.ascendPreferences.edit { it[energyKey]=index+1 } }},label={Text(label,fontSize=10.sp)},modifier=Modifier.weight(1f))
                        }
                    }
                    if(weeklyCheckins.isNotEmpty()) {
                        val averageMood=weeklyCheckins.map { it.first }.average()
                        val averageEnergy=weeklyCheckins.map { it.second }.average()
                        Divider()
                        Text("7-day average · ${weeklyCheckins.size} check-ins",color=Muted,fontSize=12.sp)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("Mood",color=Muted,fontSize=12.sp)
                            Text(String.format(java.util.Locale.US,"%.1f / 5",averageMood),color=Ink,fontWeight=FontWeight.Bold,fontSize=12.sp)
                            Text("Energy",color=Muted,fontSize=12.sp)
                            Text(String.format(java.util.Locale.US,"%.1f / 5",averageEnergy),color=Ink,fontWeight=FontWeight.Bold,fontSize=12.sp)
                        }
                    } else Text("Your 7-day averages will appear after you log check-ins.",color=Muted,fontSize=11.sp)
                    Divider()
                    Text("Daily history",color=Ink,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                    weeklyHistory.forEach { (day,m,e) ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(day.format(java.time.format.DateTimeFormatter.ofPattern("EEE")),color=Muted,fontSize=11.sp,modifier=Modifier.width(30.dp))
                            Text("Mood",color=Muted,fontSize=11.sp)
                            Text(if(m==null) "—" else "$m/5",color=Ink,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                            Text("Energy",color=Muted,fontSize=11.sp)
                            Text(if(e==null) "—" else "$e/5",color=Ink,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                        }
                    }
                    Text("Wellbeing notes are personal tracking, not a clinical assessment.",color=Muted,fontSize=11.sp)
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Green)) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("DAILY TARGET",color=Color.White.copy(alpha=.8f),fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Row(verticalAlignment=Alignment.Bottom) { Text("%,d".format(consumed),color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold); Text(" / %,d kcal".format(target),color=Color.White.copy(alpha=.8f),modifier=Modifier.padding(bottom=6.dp)) }
                    LinearProgressIndicator(progress={(consumed.toFloat()/target).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp),color=Color.White,trackColor=Color.White.copy(alpha=.25f))
                    Text(if (consumed <= target) "%,d kcal remaining".format(target - consumed) else "%,d kcal over target".format(consumed - target),color=Color.White.copy(alpha=.9f),fontSize=12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Protein goal",color=Color.White.copy(alpha=.9f),fontSize=12.sp); Text("$protein g / $proteinTarget g",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold) }
                    LinearProgressIndicator(progress={(protein.toFloat()/proteinTarget).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp),color=Color(0xFFFFD166),trackColor=Color.White.copy(alpha=.25f))
                    Text(if(protein>=proteinTarget) "Protein target reached" else "${proteinTarget-protein} g protein to goal",color=Color.White.copy(alpha=.85f),fontSize=11.sp)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Macro("Protein","${protein} g"); Macro("Carbs","${carbs} g"); Macro("Fat","${fat} g") }
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                        Column { Text("Water",color=Ink,fontWeight=FontWeight.Bold,fontSize=18.sp); Text("$waterMl ml / $waterTarget ml",color=Muted,fontSize=12.sp) }
                        Text("${(waterMl.toFloat()/waterTarget*100).toInt().coerceAtMost(999)}%",color=Green,fontWeight=FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress={(waterMl.toFloat()/waterTarget).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp),color=Green,trackColor=Mint)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={updateWater(-250)},enabled=waterMl>0,modifier=Modifier.weight(1f)) { Text("−250 ml") }
                        Button(onClick={updateWater(250)},modifier=Modifier.weight(1f)) { Text("+250 ml") }
                    }
                    Divider()
                    Text("Last 7 days",color=Ink,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                    (6 downTo 0).forEach { offset ->
                        val day = LocalDate.now().minusDays(offset.toLong())
                        val amount = waterPreferences[androidx.datastore.preferences.core.intPreferencesKey("water_ml_$day")] ?: 0
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(day.format(java.time.format.DateTimeFormatter.ofPattern("EEE")),color=Muted,fontSize=11.sp,modifier=Modifier.width(30.dp))
                            LinearProgressIndicator(progress={(amount.toFloat()/waterTarget).coerceIn(0f,1f)},modifier=Modifier.weight(1f).height(5.dp),color=Green,trackColor=Mint)
                            Text("$amount ml",color=Muted,fontSize=11.sp,modifier=Modifier.width(54.dp))
                        }
                    }
                    Text("Hydration tracking is a simple reminder; individual fluid needs vary.",color=Muted,fontSize=11.sp)
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Section("Today's meals","${loggedMeals.size}/${allMeals.size} done · ${allMeals.size-loggedMeals.size} left · $consumed/${allMeals.sumOf { it.kcal }} kcal")
                Spacer(Modifier.width(8.dp))
                IconButton(onClick={showAdd=true}) { Icon(Icons.Default.AddCircle,"Add meal",tint=Green) }
            }
        }
        items(allMeals) { meal ->
            MealCard(meal,done.contains(meal.name)){onDone(meal.name)}
            if (meal.name.startsWith("custom-")) {
                val customId = meal.name.removePrefix("custom-").toLongOrNull()
                val custom = customMeals.firstOrNull { it.id == customId }
                if (custom != null) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton(onClick={editingMeal=custom}) { Icon(Icons.Default.Edit,"Edit meal",tint=Muted); Text("Edit",color=Muted) }
                    TextButton(onClick={onRemove(custom)}) {
                        Icon(Icons.Default.Delete,"Delete meal",tint=Muted)
                        Text("Remove ${custom.title}",color=Muted)
                    }
                }
            }
        }
        item { Text("Sample meal nutrition is illustrative, not verified. Custom meal values are user-entered; check package labels and serving sizes when logging.",color=Muted,fontSize=11.sp) }
    }
    if (showAdd) AddMealDialog(onDismiss={showAdd=false},onSave={ title,time,ingredients,kcal,protein,carbs,fat ->
        onAdd(CustomMeal(date=LocalDate.now().toString(),title=title,time=time,ingredients=ingredients,kcal=kcal,protein=protein,carbs=carbs,fat=fat))
        showAdd=false
    })
    editingMeal?.let { meal -> AddMealDialog(initialMeal=meal,onDismiss={editingMeal=null},onSave={title,time,ingredients,kcal,protein,carbs,fat -> onUpdate(meal.copy(title=title,time=time,ingredients=ingredients,kcal=kcal,protein=protein,carbs=carbs,fat=fat)); editingMeal=null }) }
}
@Composable fun AddMealDialog(initialMeal:CustomMeal?=null,onDismiss:()->Unit,onSave:(String,String,String,Int,Int,Int,Int)->Unit) {
    var title by remember(initialMeal?.id) { mutableStateOf(initialMeal?.title ?: "") }
    var time by remember(initialMeal?.id) { mutableStateOf(initialMeal?.time ?: "12:00 PM") }
    var ingredients by remember(initialMeal?.id) { mutableStateOf(initialMeal?.ingredients ?: "") }
    var kcal by remember(initialMeal?.id) { mutableStateOf(initialMeal?.kcal?.toString() ?: "") }
    var protein by remember(initialMeal?.id) { mutableStateOf(initialMeal?.protein?.toString() ?: "") }
    var carbs by remember(initialMeal?.id) { mutableStateOf(initialMeal?.carbs?.toString() ?: "") }
    var fat by remember(initialMeal?.id) { mutableStateOf(initialMeal?.fat?.toString() ?: "") }
    val timeValid = runCatching { java.time.LocalTime.parse(time.trim(), java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a").toFormatter(java.util.Locale.US)) }.isSuccess
    val valid = title.isNotBlank() && kcal.toIntOrNull()?.let { it > 0 } == true && timeValid
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(initialMeal==null) "Add a meal" else "Edit meal")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=title,onValueChange={title=it.take(60)},label={Text("Meal name")},singleLine=true)
                OutlinedTextField(value=time,onValueChange={time=it.take(8)},label={Text("Time (e.g. 8:30 AM)")},singleLine=true,isError=time.isNotBlank()&&!timeValid,supportingText={if(time.isNotBlank()&&!timeValid) Text("Enter a time like 8:30 AM")})
                OutlinedTextField(value=ingredients,onValueChange={ingredients=it.take(200)},label={Text("Ingredients")},singleLine=true)
                OutlinedTextField(value=kcal,onValueChange={kcal=it.filter(Char::isDigit).take(6)},label={Text("Calories (kcal)")},singleLine=true)
                OutlinedTextField(value=protein,onValueChange={protein=it.filter(Char::isDigit).take(6)},label={Text("Protein (g)")},singleLine=true)
                OutlinedTextField(value=carbs,onValueChange={carbs=it.filter(Char::isDigit).take(6)},label={Text("Carbs (g)")},singleLine=true)
                OutlinedTextField(value=fat,onValueChange={fat=it.filter(Char::isDigit).take(6)},label={Text("Fat (g)")},singleLine=true)
                Text("Macro fields are optional; blank values are saved as 0 g. Enter values for the portion you actually ate.",color=Muted,fontSize=11.sp)
            }
        },
        confirmButton={ TextButton(enabled=valid,onClick={onSave(title.trim(),time.trim().ifBlank{"Any time"},ingredients.trim().ifBlank{"Not specified"},kcal.toInt(),protein.toIntOrNull()?:0,carbs.toIntOrNull()?:0,fat.toIntOrNull()?:0)}){Text("Save")} },
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}}
    )
}
