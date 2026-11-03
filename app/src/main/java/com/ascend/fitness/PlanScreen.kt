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


private data class WeekDaySummary(val date:LocalDate,val plannedKcal:Int,val completedMeals:Int,val totalMeals:Int)

@Composable fun Plan(p:PaddingValues,database:AscendDatabase,target:Int) {
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
    val weekEnd = weekStart.plusDays(6)
    val weekCustomMeals by database.customMealDao().observeMealsBetween(weekStart.toString(),weekEnd.toString()).collectAsState(initial=emptyList())
    val weekCompletions by database.mealCompletionDao().observeCompletionsBetween(weekStart.toString(),weekEnd.toString()).collectAsState(initial=emptyList())
    val weekSummary = (0L..6L).map { offset ->
        val day = weekStart.plusDays(offset)
        val custom = weekCustomMeals.filter { it.date == day.toString() }
        val completed = weekCompletions.filter { it.date == day.toString() }.map { it.mealId }.toSet()
        val dayMeals = meals + custom.map { Meal("custom-${it.id}",it.time,it.title,it.ingredients,it.kcal,"🍽️",it.protein,it.carbs,it.fat) }
        WeekDaySummary(day,dayMeals.sumOf { it.kcal },dayMeals.count { it.name in completed },dayMeals.size)
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
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("WEEK AT A GLANCE",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Bold)
                        TextButton(onClick={
                            val range=weekStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))+" – "+weekStart.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                            val lines=weekSummary.joinToString("\\n") { day ->
                                val label=day.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                                "$label: ${day.plannedKcal} kcal · ${day.completedMeals}/${day.totalMeals} meals completed"
                            }
                            val text="Ascend weekly meal plan — $range\\n$lines\\n\\nTotal planned: $weekCalories kcal\\nWeekly completion: $weekDone/$weekMeals meals\\nNutrition estimates are illustrative; verify ingredients and portions."
                            val send=Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_SUBJECT,"Ascend weekly meal plan — $range"); putExtra(Intent.EXTRA_TEXT,text) }
                            context.startActivity(Intent.createChooser(send,"Share weekly plan"))
                        }) { Icon(Icons.Default.Share,"Share weekly plan",tint=Green); Text("Share week") }
                    }
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
                    if(previousMeals.isEmpty()) copyFeedback="No custom meals were saved for ${selectedDate.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}."
                    else {
                        previousMeals.forEach { meal -> database.customMealDao().insert(meal.copy(id=0,date=dateKey)) }
                        copyFeedback="Copied ${previousMeals.size} custom ${if(previousMeals.size==1) "meal" else "meals"} from ${selectedDate.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}. Existing meals were kept."
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
        title={Text("Add a recipe to ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}")},
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
            Text("For ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))}",color=Muted)
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
