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

private val Green = Color(0xFF287A55)
private val Mint = Color(0xFFE7F3EB)
private val Canvas = Color(0xFFF6F8F5)
private val Ink = Color(0xFF17231D)
private val Muted = Color(0xFF6C786F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AscendApp(applicationContext, AscendDatabase.getInstance(applicationContext)) }
    }
}

@Composable
fun AscendApp(context: android.content.Context, database: AscendDatabase) {
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now().toString()
    val completedIds by database.mealCompletionDao().observeCompletedMealIds(today).collectAsState(initial = emptyList())
    val done = completedIds.toSet()
    val customMeals by database.customMealDao().observeMeals(today).collectAsState(initial = emptyList())
    val preferences by context.ascendPreferences.data.collectAsState(initial = androidx.datastore.preferences.core.emptyPreferences())
    val hostel = preferences[booleanPreferencesKey("hostel_mode")] ?: true
    val tier = preferences[stringPreferencesKey("recipe_tier")] ?: "Budget"
    val calorieTarget = preferences[androidx.datastore.preferences.core.intPreferencesKey("calorie_target")] ?: 2000
    fun toggleMeal(mealId: String) {
        scope.launch {
            val dao = database.mealCompletionDao()
            if (mealId in done) dao.removeCompletion(today, mealId)
            else dao.saveCompletion(MealCompletion(date = today, mealId = mealId))
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        Scaffold(containerColor = Canvas, bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Today", "Plan", "Recipes", "Profile").forEachIndexed { i, label ->
                    val icon = listOf(Icons.Default.Home, Icons.Default.CalendarMonth, Icons.Default.Restaurant, Icons.Default.Person)[i]
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, label) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, indicatorColor = Mint))
                }
            }
        }) { p ->
            when(tab) {
                0 -> Today(p, calorieTarget, done, customMeals, { meal -> scope.launch { database.customMealDao().insert(meal) } }, { meal -> scope.launch { database.customMealDao().update(meal) } }, ::toggleMeal, { meal -> scope.launch { database.mealCompletionDao().removeCompletion(today,"custom-${meal.id}"); database.customMealDao().delete(meal) } })
                1 -> Plan(p, database, calorieTarget)
                2 -> Recipes(p, tier, hostel, database) { selected -> scope.launch { context.ascendPreferences.edit { it[stringPreferencesKey("recipe_tier")] = selected } } }
                else -> Profile(p, hostel, tier, calorieTarget, { enabled -> scope.launch { context.ascendPreferences.edit { it[booleanPreferencesKey("hostel_mode")] = enabled } } }, { target -> scope.launch { context.ascendPreferences.edit { it[androidx.datastore.preferences.core.intPreferencesKey("calorie_target")] = target } } })
            }
        }
    }
}

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

@Composable private fun Header(title:String, subtitle:String) {
    Column {
        Text("ASCEND", color=Green, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
        Text(title, color=Ink, fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text(subtitle, color=Muted)
    }
}
@Composable private fun Section(title:String, trailing:String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color=Ink, fontSize=18.sp, fontWeight=FontWeight.Bold)
        Text(trailing, color=Muted, fontSize=12.sp)
    }
}
@Composable private fun Macro(label:String,value:String) {
    Column { Text(label,color=Color.White.copy(alpha=.8f),fontSize=11.sp); Text(value,color=Color.White,fontWeight=FontWeight.Bold) }
}
@Composable private fun MealCard(meal:Meal, checked:Boolean, onCheck:()->Unit) {
    Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.White)) {
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
@Composable private fun Today(p:PaddingValues, target:Int, done:Set<String>, customMeals:List<CustomMeal>, onAdd:(CustomMeal)->Unit, onUpdate:(CustomMeal)->Unit, onDone:(String)->Unit, onRemove:(CustomMeal)->Unit) {
    var editingMeal by remember { mutableStateOf<CustomMeal?>(null) }
    var showAdd by remember { mutableStateOf(false) }
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
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Green)) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("DAILY TARGET",color=Color.White.copy(alpha=.8f),fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Row(verticalAlignment=Alignment.Bottom) { Text("%,d".format(consumed),color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold); Text(" / %,d kcal".format(target),color=Color.White.copy(alpha=.8f),modifier=Modifier.padding(bottom=6.dp)) }
                    LinearProgressIndicator(progress={(consumed.toFloat()/target).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp),color=Color.White,trackColor=Color.White.copy(alpha=.25f))
                    Text(if (consumed <= target) "%,d kcal remaining".format(target - consumed) else "%,d kcal over target".format(consumed - target),color=Color.White.copy(alpha=.9f),fontSize=12.sp)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Macro("Protein","${protein} g"); Macro("Carbs","${carbs} g"); Macro("Fat","${fat} g") }
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
@Composable private fun AddMealDialog(initialMeal:CustomMeal?=null,onDismiss:()->Unit,onSave:(String,String,String,Int,Int,Int,Int)->Unit) {
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
private data class WeekDaySummary(val date:LocalDate,val plannedKcal:Int,val completedMeals:Int,val totalMeals:Int)

@Composable private fun Plan(p:PaddingValues,database:AscendDatabase,target:Int) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCopyDatePicker by remember { mutableStateOf(false) }
    var copyTargetDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var showAdd by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<CustomMeal?>(null) }
    var showRecipePicker by remember { mutableStateOf(false) }
    var selectedPlanRecipe by remember { mutableStateOf<Recipe?>(null) }
    var planRecipeServings by remember { mutableIntStateOf(1) }
    var copyFeedback by remember { mutableStateOf<String?>(null) }
    val dateKey = selectedDate.toString()
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value-1).toLong())
    var weekSummary by remember { mutableStateOf<List<WeekDaySummary>>(emptyList()) }
    LaunchedEffect(weekStart) {
        weekSummary = (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            val dayKey = day.toString()
            val custom = database.customMealDao().getMeals(dayKey)
            val completed = database.mealCompletionDao().getCompletedMealIds(dayKey).toSet()
            val dayMeals = meals + custom.map { Meal("custom-${it.id}",it.time,it.title,it.ingredients,it.kcal,"🍽️",it.protein,it.carbs,it.fat) }
            WeekDaySummary(day,dayMeals.sumOf { it.kcal },dayMeals.count { it.name in completed },dayMeals.size)
        }
    }
    val completedIds by database.mealCompletionDao().observeCompletedMealIds(dateKey).collectAsState(initial=emptyList())
    val customMeals by database.customMealDao().observeMeals(dateKey).collectAsState(initial=emptyList())
    val scope = rememberCoroutineScope()
    val completed = completedIds.toSet()
    val allMeals = sortMealsByTime(meals + customMeals.map { Meal("custom-${it.id}",it.time,it.title,it.ingredients,it.kcal,"🍽️",it.protein,it.carbs,it.fat) })
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Meal plan","Plan and track meals by date") }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    IconButton(onClick={selectedDate=selectedDate.minusDays(1)}) { Icon(Icons.Default.ChevronLeft,"Previous day") }
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE")),color=Ink,fontWeight=FontWeight.Bold)
                        TextButton(onClick={showDatePicker=true},contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)) {
                            Text(selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),color=Green)
                            Icon(Icons.Default.CalendarMonth,"Choose date",tint=Green,modifier=Modifier.size(16.dp))
                        }
                        if(selectedDate==LocalDate.now()) Text("Today",color=Green,fontSize=12.sp)
                        else TextButton(onClick={selectedDate=LocalDate.now()}) { Text("Today",color=Green) }
                    }
                    IconButton(onClick={selectedDate=selectedDate.plusDays(1)}) { Icon(Icons.Default.ChevronRight,"Next day") }
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()) {
                IconButton(onClick={selectedDate=selectedDate.minusWeeks(1)}) { Icon(Icons.Default.ChevronLeft,"Previous week",tint=Green) }
                Text(weekStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))+" – "+weekStart.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("MMM d")),fontWeight=FontWeight.SemiBold,color=Ink)
                IconButton(onClick={selectedDate=selectedDate.plusWeeks(1)}) { Icon(Icons.Default.ChevronRight,"Next week",tint=Green) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                (0L..6L).forEach { offset ->
                    val day=weekStart.plusDays(offset)
                    val selected=day==selectedDate
                    Surface(onClick={selectedDate=day},shape=RoundedCornerShape(14.dp),color=if(selected) Green else Color.White,contentColor=if(selected) Color.White else Ink) {
                        Column(Modifier.width(42.dp).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(day.format(java.time.format.DateTimeFormatter.ofPattern("EE")),fontSize=11.sp,color=if(selected) Color.White.copy(alpha=.85f) else Muted)
                            Text(day.dayOfMonth.toString(),fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            val weekCalories=weekSummary.sumOf { it.plannedKcal }
            val weekDone=weekSummary.sumOf { it.completedMeals }
            val weekMeals=weekSummary.sumOf { it.totalMeals }
            Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("WEEK AT A GLANCE",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Column {
                            Text("%,d".format(weekCalories),fontSize=22.sp,fontWeight=FontWeight.Bold,color=Ink)
                            Text("planned kcal",fontSize=11.sp,color=Muted)
                        }
                        Column(horizontalAlignment=Alignment.End) {
                            Text("$weekDone / $weekMeals",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Green)
                            Text("meals completed",fontSize=11.sp,color=Muted)
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.Bottom) {
                        weekSummary.forEach { day ->
                            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.fillMaxWidth().height(44.dp),contentAlignment=Alignment.BottomCenter) {
                                    Box(Modifier.fillMaxWidth().height((44f*(day.completedMeals.toFloat()/day.totalMeals.coerceAtLeast(1))).coerceAtLeast(if(day.completedMeals>0) 4f else 0f).dp).background(Green.copy(alpha=.8f),RoundedCornerShape(4.dp)))
                                }
                                Text(day.date.format(java.time.format.DateTimeFormatter.ofPattern("EE")),fontSize=10.sp,color=Muted)
                            }
                        }
                    }
                    val daysWithMeals=weekSummary.count { it.plannedKcal>0 }
                    val underTargetDays=weekSummary.count { it.plannedKcal in 1..target }
                    val averageKcal=if(daysWithMeals>0) weekSummary.sumOf { it.plannedKcal }/daysWithMeals else 0
                    val completionRate=if(weekMeals>0) (100*weekDone/weekMeals) else 0
                    Text("Weekly insights",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).background(Canvas,RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text("$underTargetDays / $daysWithMeals",fontWeight=FontWeight.Bold,color=Green,fontSize=18.sp)
                            Text("planned days at or below target",fontSize=10.sp,color=Muted)
                        }
                        Column(Modifier.weight(1f).background(Canvas,RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text(if(daysWithMeals>0) "%,d".format(averageKcal) else "—",fontWeight=FontWeight.Bold,color=Ink,fontSize=18.sp)
                            Text("average kcal per planned day",fontSize=10.sp,color=Muted)
                        }
                        Column(Modifier.weight(1f).background(Canvas,RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text("$completionRate%",fontWeight=FontWeight.Bold,color=Green,fontSize=18.sp)
                            Text("meal completion",fontSize=10.sp,color=Muted)
                        }
                    }
                    Text("Planned calories by day",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.Bottom) {
                        weekSummary.forEach { day ->
                            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Text(if(day.plannedKcal>0) (day.plannedKcal/100).toString()+"k" else "—",fontSize=9.sp,color=Muted)
                                Box(Modifier.fillMaxWidth().height(44.dp),contentAlignment=Alignment.BottomCenter) {
                                    Box(Modifier.fillMaxWidth().height((44f*(day.plannedKcal.toFloat()/target.coerceAtLeast(1)).coerceIn(0f,1f)).coerceAtLeast(if(day.plannedKcal>0) 3f else 0f).dp).background(if(day.plannedKcal>target) Color(0xFFD99B48) else Mint,RoundedCornerShape(4.dp)))
                                }
                                Text(day.date.format(java.time.format.DateTimeFormatter.ofPattern("EE")),fontSize=10.sp,color=Muted)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                TextButton(onClick={
                    val dateLabel=selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    val mealLines=allMeals.joinToString("\\n") { meal -> "${if(meal.name in completed) "✓" else "○"} ${meal.time} — ${meal.title} (${meal.kcal} kcal; P ${meal.protein}g, C ${meal.carbs}g, F ${meal.fat}g)" }
                    val totalKcal=allMeals.sumOf { it.kcal }
                    val text="Ascend meal plan — $dateLabel\\n$mealLines\\n\\nPlanned total: $totalKcal kcal\\nNutrition values are estimates; verify ingredients and portions."
                    val send=Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_SUBJECT,"Ascend meal plan — $dateLabel"); putExtra(Intent.EXTRA_TEXT,text) }
                    context.startActivity(Intent.createChooser(send,"Share meal plan"))
                }) { Icon(Icons.Default.Share,"Share plan",tint=Green); Text("Share plan") }
                TextButton(onClick={
                    val dateLabel=selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    val ingredientLines=allMeals.joinToString("\\n") { meal -> "• ${meal.title}: ${meal.ingredients}" }
                    val text="Ascend grocery checklist — $dateLabel\\n$ingredientLines\\n\\nIngredient notes are copied from meal entries; quantities may be incomplete. Review portions and pantry stock before shopping."
                    val send=Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_SUBJECT,"Grocery list — $dateLabel"); putExtra(Intent.EXTRA_TEXT,text) }
                    context.startActivity(Intent.createChooser(send,"Share grocery list"))
                }) { Icon(Icons.Default.ShoppingCart,"Share grocery list",tint=Green); Text("Ingredients") }
                TextButton(onClick={copyTargetDate=selectedDate.plusDays(1);showCopyDatePicker=true}) {
                    Icon(Icons.Default.Event,"Copy to date",tint=Green); Text("Copy to date")
                }
                TextButton(onClick={scope.launch {
                    val previousDate=selectedDate.minusDays(1).toString()
                    val previousMeals=database.customMealDao().getMeals(previousDate)
                    if(previousMeals.isEmpty()) copyFeedback="No custom meals were saved for ${selectedDate.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern(\"MMM d\"))}."
                    else {
                        previousMeals.forEach { meal -> database.customMealDao().insert(meal.copy(id=0,date=dateKey)) }
                        copyFeedback="Copied ${previousMeals.size} custom ${if(previousMeals.size==1) "meal" else "meals"} from ${selectedDate.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern(\"MMM d\"))}. Existing meals were kept."
                    }
                }}) { Icon(Icons.Default.ContentCopy,"Copy previous day",tint=Green); Text("Copy previous day") }
            }
        }
        item {
            val loggedMeals = allMeals.filter { it.name in completed }
            val calories = loggedMeals.sumOf { it.kcal }
            val protein = loggedMeals.sumOf { it.protein }
            val carbs = loggedMeals.sumOf { it.carbs }
            val fat = loggedMeals.sumOf { it.fat }
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Green)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("DAILY PROGRESS",color=Color.White.copy(alpha=.8f),fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Row(verticalAlignment=Alignment.Bottom) {
                        Text("%,d".format(calories),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                        Text(" / %,d kcal".format(target),color=Color.White.copy(alpha=.8f),modifier=Modifier.padding(bottom=5.dp))
                    }
                    LinearProgressIndicator(progress={(calories.toFloat()/target.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp),color=Color.White,trackColor=Color.White.copy(alpha=.25f))
                    Text(if (calories <= target) "%,d kcal remaining".format(target - calories) else "%,d kcal over target".format(calories - target), color=Color.White.copy(alpha=.9f), fontSize=12.sp)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Macro("Protein","${protein} g")
                        Macro("Carbs","${carbs} g")
                        Macro("Fat"," ${fat} g")
                    }
                    Text("${loggedMeals.size} of ${allMeals.size} meals completed · ${allMeals.sumOf { it.kcal }} kcal planned",color=Color.White.copy(alpha=.9f),fontSize=12.sp)
                    Text("Planned macros · P ${allMeals.sumOf { it.protein }}g · C ${allMeals.sumOf { it.carbs }}g · F ${allMeals.sumOf { it.fat }}g",color=Color.White.copy(alpha=.8f),fontSize=11.sp)
                    Text("Macro gap · P ${allMeals.sumOf { it.protein }-protein}g · C ${allMeals.sumOf { it.carbs }-carbs}g · F ${allMeals.sumOf { it.fat }-fat}g",color=Color.White.copy(alpha=.8f),fontSize=11.sp)
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Section("Meals for this day"," ${allMeals.size} meals")
                Spacer(Modifier.width(4.dp))
                TextButton(onClick={showRecipePicker=true}) { Icon(Icons.Default.Restaurant,"Add recipe",tint=Green); Text("Recipe") }
                IconButton(onClick={showAdd=true}) { Icon(Icons.Default.AddCircle,"Add meal",tint=Green) }
            }
        }
        items(allMeals) { meal ->
            MealCard(meal,meal.name in completed) {
                scope.launch {
                    val dao=database.mealCompletionDao()
                    if(meal.name in completed) dao.removeCompletion(dateKey,meal.name)
                    else dao.saveCompletion(MealCompletion(dateKey,meal.name))
                }
            }
            if (meal.name.startsWith("custom-")) {
                val customId=meal.name.removePrefix("custom-").toLongOrNull()
                val custom=customMeals.firstOrNull { it.id==customId }
                if(custom!=null) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton(onClick={editingMeal=custom}) { Icon(Icons.Default.Edit,"Edit meal",tint=Muted); Text("Edit",color=Muted) }
                    TextButton(onClick={scope.launch { database.mealCompletionDao().removeCompletion(dateKey,"custom-${custom.id}"); database.customMealDao().delete(custom) }}) {
                        Icon(Icons.Default.Delete,"Delete meal",tint=Muted)
                        Text("Remove",color=Muted)
                    }
                }
            }
        }
        item { Text("Sample meal nutrition is illustrative, not verified. Custom meal values are user-entered; check package labels and serving sizes when logging.",color=Muted,fontSize=11.sp) }
    }
    if(showCopyDatePicker) {
        val copyPickerState=rememberDatePickerState(initialSelectedDateMillis=copyTargetDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest={showCopyDatePicker=false},
            confirmButton={TextButton(onClick={
                copyPickerState.selectedDateMillis?.let { millis -> copyTargetDate=java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
                showCopyDatePicker=false
                scope.launch {
                    val sourceMeals=database.customMealDao().getMeals(dateKey)
                    if(sourceMeals.isEmpty()) copyFeedback="No custom meals to copy from " + selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")) + "."
                    else if(copyTargetDate==selectedDate) copyFeedback="Choose a different date to copy these meals."
                    else {
                        sourceMeals.forEach { meal -> database.customMealDao().insert(meal.copy(id=0,date=copyTargetDate.toString())) }
                        copyFeedback="Copied " + sourceMeals.size + " custom meal(s) to " + copyTargetDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) + ". Existing meals were kept."
                    }
                }
            }) { Text("Copy meals") }},
            dismissButton={TextButton(onClick={showCopyDatePicker=false}) { Text("Cancel") }}
        ) { DatePicker(state=copyPickerState) }
    }
    if(showDatePicker) {
        val pickerState=rememberDatePickerState(initialSelectedDateMillis=selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest={showDatePicker=false},
            confirmButton={TextButton(onClick={
                pickerState.selectedDateMillis?.let { millis -> selectedDate=java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
                showDatePicker=false
            }) { Text("Go to date") }},
            dismissButton={TextButton(onClick={showDatePicker=false}) { Text("Cancel") }}
        ) { DatePicker(state=pickerState) }
    }
    if(showAdd) AddMealDialog(onDismiss={showAdd=false},onSave={title,time,ingredients,kcal,protein,carbs,fat ->
        scope.launch {
            database.customMealDao().insert(CustomMeal(date=dateKey,title=title,time=time,ingredients=ingredients,kcal=kcal,protein=protein,carbs=carbs,fat=fat))
        }
        showAdd=false
    })
    editingMeal?.let { meal -> AddMealDialog(initialMeal=meal,onDismiss={editingMeal=null},onSave={title,time,ingredients,kcal,protein,carbs,fat ->
        scope.launch {
            database.customMealDao().update(meal.copy(title=title,time=time,ingredients=ingredients,kcal=kcal,protein=protein,carbs=carbs,fat=fat))
        }
        editingMeal=null
    }) }
    if(showRecipePicker) AlertDialog(
        onDismissRequest={showRecipePicker=false},
        title={Text("Add a recipe to ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern(\"MMM d\"))}")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            recipes.forEach { recipe ->
                OutlinedButton(onClick={showRecipePicker=false;selectedPlanRecipe=recipe;planRecipeServings=1},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.Start) {
                        Text(recipe.title,color=Ink,fontWeight=FontWeight.SemiBold)
                        Text("${recipe.kcal} kcal · ~₹${recipe.cost}/serving",color=Muted,fontSize=12.sp)
                    }
                }
            }
        }},
        confirmButton={TextButton(onClick={showRecipePicker=false}){Text("Close")}}
    )
    copyFeedback?.let { message -> AlertDialog(onDismissRequest={copyFeedback=null},title={Text("Plan copied")},text={Text(message)},confirmButton={TextButton(onClick={copyFeedback=null}){Text("OK")}}) }
    selectedPlanRecipe?.let { recipe -> AlertDialog(
        onDismissRequest={selectedPlanRecipe=null},
        title={Text("Add ${recipe.title}")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("For ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern(\"EEEE, MMM d\"))}",color=Muted)
            Text("Servings: $planRecipeServings",fontWeight=FontWeight.SemiBold)
            Slider(value=planRecipeServings.toFloat(),onValueChange={planRecipeServings=it.toInt()},valueRange=1f..5f,steps=3)
            Text("${recipe.kcal*planRecipeServings} kcal · P ${recipe.protein*planRecipeServings}g · C ${recipe.carbs*planRecipeServings}g · F ${recipe.fat*planRecipeServings}g",color=Green,fontWeight=FontWeight.SemiBold)
            Text("Estimated cost: ₹${recipe.cost*planRecipeServings}",color=Muted)
        }},
        confirmButton={TextButton(onClick={
            val ingredients=when(recipe.title) {
                "Banana oats bowl" -> "½ cup oats, 1 banana, ¾ cup milk"
                "Chickpea rice bowl" -> "¾ cup cooked chickpeas, 1 cup cooked rice, cucumber, lemon"
                "Peanut banana bites" -> "1 banana, 1 tbsp peanuts"
                "Moong dal khichdi" -> "¼ cup moong dal, ¼ cup rice, spices, vegetables"
                else -> "1 roti/wrap, paneer, mixed vegetables, yogurt or chutney"
            }
            scope.launch { database.customMealDao().insert(CustomMeal(date=dateKey,title=if(planRecipeServings==1) recipe.title else "${recipe.title} · $planRecipeServings servings",time="Any time",ingredients="$planRecipeServings serving(s): $ingredients",kcal=recipe.kcal*planRecipeServings,protein=recipe.protein*planRecipeServings,carbs=recipe.carbs*planRecipeServings,fat=recipe.fat*planRecipeServings)) }
            selectedPlanRecipe=null
        }){Text("Add to plan")}},
        dismissButton={TextButton(onClick={selectedPlanRecipe=null}){Text("Cancel")}}
    ) }
}
private data class Recipe(val title:String,val timing:String,val kcal:Int,val cost:Int,val tags:String,val emoji:String,val protein:Int,val carbs:Int,val fat:Int)
private val recipes=listOf(
    Recipe("Banana oats bowl","5 min · No cook",380,32,"Hostel-friendly · Breakfast","🥣",14,62,9),
    Recipe("Chickpea rice bowl","15 min · One pot",520,48,"High protein · Lunch","🍛",18,88,8),
    Recipe("Peanut banana bites","3 min · No cook",180,18,"Snack · No cook","🍌",5,27,8),
    Recipe("Moong dal khichdi","20 min · One pot",420,36,"Comfort food · Dinner","🍲",17,72,7),
    Recipe("Paneer veggie wrap","12 min · Pan",460,72,"Vegetarian · Lunch","🌯",22,48,18)
)
@Composable private fun Recipes(p:PaddingValues,tier:String,hostel:Boolean,database:AscendDatabase,onTier:(String)->Unit) {
    val scope = rememberCoroutineScope()
    val savedIds by database.savedRecipeDao().observeSavedRecipeIds().collectAsState(initial=emptyList())
    var savedOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Recommended") }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    val maxCost = when(tier) { "Budget" -> 50; "Standard" -> 75; else -> Int.MAX_VALUE }
    val query = searchQuery.trim()
    val filteredRecipes = recipes.filter { it.cost <= maxCost && (!savedOnly || it.title in savedIds) && (!hostel || it.timing.contains("No cook") || it.timing.contains("One pot")) && (query.isBlank() || query.split(Regex("\\s+")).filter { term -> term.isNotBlank() }.all { term -> it.title.contains(term, ignoreCase=true) || it.tags.contains(term, ignoreCase=true) || it.timing.contains(term, ignoreCase=true) }) }
    val visibleRecipes = when(sortBy) { "Lowest cost" -> filteredRecipes.sortedWith(compareBy<Recipe> { it.cost }.thenBy { it.title }); "Lowest calories" -> filteredRecipes.sortedWith(compareBy<Recipe> { it.kcal }.thenBy { it.title }); "Highest protein" -> filteredRecipes.sortedWith(compareByDescending<Recipe> { it.protein }.thenBy { it.title }); else -> filteredRecipes }
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Recipe library","Simple, student-friendly meals") }
        item { OutlinedTextField(value=searchQuery,onValueChange={searchQuery=it},modifier=Modifier.fillMaxWidth(),label={Text("Search recipes, tags, or meal time")},singleLine=true,leadingIcon={Icon(Icons.Default.Search,"Search")},trailingIcon={if(searchQuery.isNotEmpty()) IconButton(onClick={searchQuery=""}) { Icon(Icons.Default.Close,"Clear search") }}) }
        item { Text("${visibleRecipes.size} ${if(visibleRecipes.size==1) "recipe" else "recipes"} found",color=Muted,fontSize=12.sp) }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Recommended","Lowest cost","Lowest calories","Highest protein").forEach { option -> FilterChip(selected=sortBy==option,onClick={sortBy=option},label={Text(option)}) } } }
        item {
            Text("Your budget",color=Ink,fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=8.dp)) {
                listOf("Budget","Standard","Premium").forEach { FilterChip(selected=tier==it,onClick={onTier(it)},label={Text(it)}) }
            }
        }
        item { FilterChip(selected=savedOnly,onClick={savedOnly=!savedOnly},label={Text(if(savedOnly) "Saved only" else "Saved recipes (${savedIds.size})")}) }
        item { if(query.isNotBlank() || savedOnly || sortBy != "Recommended") TextButton(onClick={searchQuery="";savedOnly=false;sortBy="Recommended"}) { Text("Reset filters") } }
        item { if(hostel) Text("Hostel mode: showing no-cook and one-pot options.",color=Muted,fontSize=12.sp) }
        items(visibleRecipes,key={it.title}) { r ->
            Card(modifier=Modifier.clickable { selectedRecipe=r },shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(Mint,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){Text(r.emoji,fontSize=26.sp)}
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(r.title,color=Ink,fontWeight=FontWeight.SemiBold)
                        Text(r.timing,color=Muted,fontSize=12.sp)
                        Text(r.tags,color=Green,fontSize=11.sp)
                        Text("${r.kcal} kcal · ~₹${r.cost}/serving",color=Muted,fontSize=12.sp)
                        Text("P ${r.protein}g · C ${r.carbs}g · F ${r.fat}g",color=Green,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                        Text("View recipe",color=Green,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                    }
                    IconButton(onClick={scope.launch { if(r.title in savedIds) database.savedRecipeDao().remove(r.title) else database.savedRecipeDao().save(SavedRecipe(r.title)) }}) {
                        Icon(if(r.title in savedIds) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,if(r.title in savedIds) "Remove saved recipe" else "Save recipe",tint=Green)
                    }
                }
            }
        }
        item { if(visibleRecipes.isEmpty()) Column(verticalArrangement=Arrangement.spacedBy(6.dp)) { Text("No recipes match your current filters.",color=Muted); TextButton(onClick={searchQuery="";savedOnly=false;sortBy="Recommended"}) { Text("Clear search and filters") } } }
        item { Text("Costs are illustrative estimates, not live local prices. Nutrition values are sample estimates, not verified; check ingredient labels and serving sizes.",color=Muted,fontSize=11.sp) }
    }
    selectedRecipe?.let { recipe ->
        var servings by remember(recipe.title) { mutableIntStateOf(1) }
        val ingredients = when(recipe.title) {
            "Banana oats bowl" -> "½ cup oats, 1 banana, ¾ cup milk"
            "Chickpea rice bowl" -> "¾ cup cooked chickpeas, 1 cup cooked rice, cucumber, lemon"
            "Peanut banana bites" -> "1 banana, 1 tbsp peanuts"
            "Moong dal khichdi" -> "¼ cup moong dal, ¼ cup rice, spices, vegetables"
            else -> "1 roti/wrap, paneer, mixed vegetables, yogurt or chutney"
        }
        val steps = when(recipe.title) {
            "Banana oats bowl" -> "Mix oats and milk; top with sliced banana. Let soften or chill overnight."
            "Chickpea rice bowl" -> "Warm chickpeas with spices. Serve over rice with cucumber and lemon."
            "Peanut banana bites" -> "Slice banana and top with crushed peanuts."
            "Moong dal khichdi" -> "Rinse dal and rice. Simmer with spices and water until soft."
            else -> "Cook paneer and vegetables with spices. Fill the wrap, roll, and toast briefly."
        }
        AlertDialog(
            onDismissRequest={selectedRecipe=null},
            title={Text(recipe.title)},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("${recipe.timing} · ${recipe.kcal * servings} kcal · estimated ₹${recipe.cost * servings} total",color=Muted)
                Text("Servings: $servings",fontWeight=FontWeight.SemiBold)
                Slider(value=servings.toFloat(),onValueChange={servings=it.toInt()},valueRange=1f..5f,steps=3)
                Text("Per serving: ${recipe.kcal} kcal · P ${recipe.protein}g · C ${recipe.carbs}g · F ${recipe.fat}g",color=Green,fontWeight=FontWeight.SemiBold)
                Text("Total: P ${recipe.protein * servings}g · C ${recipe.carbs * servings}g · F ${recipe.fat * servings}g",color=Green,fontWeight=FontWeight.SemiBold)
                Text("Ingredients (per serving)",fontWeight=FontWeight.Bold)
                Text(ingredients)
                Text("Method",fontWeight=FontWeight.Bold)
                Text(steps)
                Text("Nutrition and cost are estimates; adjust portions and ingredients to your needs.",color=Muted,fontSize=12.sp)
            }},
            confirmButton={
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={
                        scope.launch {
                            database.customMealDao().insert(
                                CustomMeal(
                                    date=LocalDate.now().toString(),
                                    title=if(servings==1) recipe.title else "${recipe.title} · $servings servings",
                                    time="Any time",
                                    ingredients="$servings serving(s): $ingredients",
                                    kcal=recipe.kcal * servings,
                                    protein=recipe.protein * servings,
                                    carbs=recipe.carbs * servings,
                                    fat=recipe.fat * servings
                                )
                            )
                            selectedRecipe=null
                        }
                    }) { Text("Add to today") }
                    TextButton(onClick={selectedRecipe=null}) { Text("Close") }
                }
            }
        )
    }
}
@Composable private fun Profile(p:PaddingValues,hostel:Boolean,tier:String,target:Int,onHostel:(Boolean)->Unit,onTarget:(Int)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Your profile","Preferences for a plan that fits your life") }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
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
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Hostel mode",color=Ink,fontWeight=FontWeight.Bold); Text("Prioritize kettle/no-cook meals and simple equipment.",color=Muted,fontSize=12.sp) }
                    Switch(checked=hostel,onCheckedChange=onHostel)
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
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
@Composable private fun ProfileRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,color=Muted); Text(value,color=Ink,fontWeight=FontWeight.Medium) }
}
