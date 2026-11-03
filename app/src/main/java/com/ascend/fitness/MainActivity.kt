package com.ascend.fitness

import android.os.Bundle
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
                0 -> Today(p, calorieTarget, done, customMeals, { meal -> scope.launch { database.customMealDao().insert(meal) } }, ::toggleMeal, { meal -> scope.launch { database.mealCompletionDao().removeCompletion(today,"custom-${meal.id}"); database.customMealDao().delete(meal) } })
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
@Composable private fun Today(p:PaddingValues, target:Int, done:Set<String>, customMeals:List<CustomMeal>, onAdd:(CustomMeal)->Unit, onDone:(String)->Unit, onRemove:(CustomMeal)->Unit) {
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
                    TextButton(onClick={onRemove(custom)}) {
                        Icon(Icons.Default.Delete,"Delete meal",tint=Muted)
                        Text("Remove ${custom.title}",color=Muted)
                    }
                }
            }
        }
        item { Text("Sample meal nutrition is illustrative, not verified. Custom meal values are user-entered.",color=Muted,fontSize=11.sp) }
    }
    if (showAdd) AddMealDialog(onDismiss={showAdd=false},onSave={ title,time,ingredients,kcal,protein,carbs,fat ->
        onAdd(CustomMeal(date=LocalDate.now().toString(),title=title,time=time,ingredients=ingredients,kcal=kcal,protein=protein,carbs=carbs,fat=fat))
        showAdd=false
    })
}
@Composable private fun AddMealDialog(onDismiss:()->Unit,onSave:(String,String,String,Int,Int,Int,Int)->Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("12:00 PM") }
    var ingredients by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    val timeValid = runCatching { java.time.LocalTime.parse(time.trim(), java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mm a").toFormatter(java.util.Locale.US)) }.isSuccess
    val valid = title.isNotBlank() && kcal.toIntOrNull()?.let { it > 0 } == true && timeValid
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Add a meal")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=title,onValueChange={title=it.take(60)},label={Text("Meal name")},singleLine=true)
                OutlinedTextField(value=time,onValueChange={time=it.take(8)},label={Text("Time (e.g. 8:30 AM)")},singleLine=true,isError=time.isNotBlank()&&!timeValid,supportingText={if(time.isNotBlank()&&!timeValid) Text("Enter a time like 8:30 AM")})
                OutlinedTextField(value=ingredients,onValueChange={ingredients=it.take(200)},label={Text("Ingredients")},singleLine=true)
                OutlinedTextField(value=kcal,onValueChange={kcal=it.filter(Char::isDigit).take(6)},label={Text("Calories (kcal)")},singleLine=true)
                OutlinedTextField(value=protein,onValueChange={protein=it.filter(Char::isDigit).take(6)},label={Text("Protein (g)")},singleLine=true)
                OutlinedTextField(value=carbs,onValueChange={carbs=it.filter(Char::isDigit).take(6)},label={Text("Carbs (g)")},singleLine=true)
                OutlinedTextField(value=fat,onValueChange={fat=it.filter(Char::isDigit).take(6)},label={Text("Fat (g)")},singleLine=true)
            }
        },
        confirmButton={ TextButton(enabled=valid,onClick={onSave(title.trim(),time.trim().ifBlank{"Any time"},ingredients.trim().ifBlank{"Not specified"},kcal.toInt(),protein.toIntOrNull()?:0,carbs.toIntOrNull()?:0,fat.toIntOrNull()?:0)}){Text("Save")} },
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}}
    )
}
@Composable private fun Plan(p:PaddingValues,database:AscendDatabase,target:Int) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAdd by remember { mutableStateOf(false) }
    val dateKey = selectedDate.toString()
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
                        Text(selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),color=Muted)
                        if(selectedDate==LocalDate.now()) Text("Today",color=Green,fontSize=12.sp)
                        else TextButton(onClick={selectedDate=LocalDate.now()}) { Text("Today",color=Green) }
                    }
                    IconButton(onClick={selectedDate=selectedDate.plusDays(1)}) { Icon(Icons.Default.ChevronRight,"Next day") }
                }
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
                Spacer(Modifier.width(8.dp))
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
                    TextButton(onClick={scope.launch { database.mealCompletionDao().removeCompletion(dateKey,"custom-${custom.id}"); database.customMealDao().delete(custom) }}) {
                        Icon(Icons.Default.Delete,"Delete meal",tint=Muted)
                        Text("Remove",color=Muted)
                    }
                }
            }
        }
        item { Text("Sample meal nutrition is illustrative, not verified.",color=Muted,fontSize=11.sp) }
    }
    if(showAdd) AddMealDialog(onDismiss={showAdd=false},onSave={title,time,ingredients,kcal,protein,carbs,fat ->
        scope.launch {
            database.customMealDao().insert(CustomMeal(date=dateKey,title=title,time=time,ingredients=ingredients,kcal=kcal,protein=protein,carbs=carbs,fat=fat))
        }
        showAdd=false
    })
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
        item { Text("Costs are illustrative estimates, not live local prices. Nutrition values are sample estimates, not verified.",color=Muted,fontSize=11.sp) }
    }
    selectedRecipe?.let { recipe ->
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
                Text("${recipe.timing} · ${recipe.kcal} kcal · estimated ₹${recipe.cost}/serving",color=Muted)
                Text("Protein ${recipe.protein}g · Carbs ${recipe.carbs}g · Fat ${recipe.fat}g",color=Green,fontWeight=FontWeight.SemiBold)
                Text("Ingredients",fontWeight=FontWeight.Bold)
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
                                    title=recipe.title,
                                    time="Any time",
                                    ingredients=ingredients,
                                    kcal=recipe.kcal,
                                    protein=recipe.protein,
                                    carbs=recipe.carbs,
                                    fat=recipe.fat
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
                    ProfileRow("Goal","Healthy weight management")
                    ProfileRow("Daily energy target","%,d kcal".format(target))
                    ProfileRow("Diet","Vegetarian")
                    ProfileRow("Budget",when(tier) { "Budget" -> "Up to ₹50/meal"; "Standard" -> "Up to ₹75/meal"; else -> "Flexible" })
                    ProfileRow("Recipe tier",tier)
                    Text("Daily calorie target",color=Muted,fontSize=12.sp)
                    Slider(value=target.toFloat(),onValueChange={ onTarget(it.toInt()) },valueRange=1000f..4000f,steps=29)
                    Text("Adjust from 1,000 to 4,000 kcal. This is a tracking target, not a personalized medical recommendation.",color=Muted,fontSize=11.sp)
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
                    Text("Personalized plans, expanded recipes, and expert consultations.",color=Muted)
                    Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Explore membership · ₹99/month")}
                }
            }
        }
        item { Text("Hostel mode and recipe budget are saved on this device. Membership and expert booking are not connected yet.",color=Muted,fontSize=11.sp) }
    }
}
@Composable private fun ProfileRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,color=Muted); Text(value,color=Ink,fontWeight=FontWeight.Medium) }
}
