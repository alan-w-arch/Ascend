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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AscendApp(applicationContext, AscendDatabase.getInstance(applicationContext)) }
    }
}

@Composable
fun AscendApp(context: android.content.Context, database: AscendDatabase) {
    var tab by remember { mutableIntStateOf(0) }
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF9BD4B1), background = Color(0xFF101512),
        surface = Color(0xFF1A211D), onSurface = Color(0xFFE6EDE8),
        onBackground = Color(0xFFE6EDE8), secondary = Color(0xFFB4CDBD)
    ) else lightColorScheme(primary = Green, background = Canvas, surface = Color.White)
    val scope = rememberCoroutineScope()
    val today = LocalDate.now().toString()
    val completedIds by database.mealCompletionDao().observeCompletedMealIds(today).collectAsState(initial = emptyList())
    val done = completedIds.toSet()
    val customMeals by database.customMealDao().observeMeals(today).collectAsState(initial = emptyList())
    val preferences by context.ascendPreferences.data.collectAsState(initial = androidx.datastore.preferences.core.emptyPreferences())
    val hostel = preferences[booleanPreferencesKey("hostel_mode")] ?: true
    val tier = preferences[stringPreferencesKey("recipe_tier")] ?: "Budget"
    val calorieTarget = preferences[androidx.datastore.preferences.core.intPreferencesKey("calorie_target")] ?: 2000
    val proteinTarget = preferences[androidx.datastore.preferences.core.intPreferencesKey("protein_target")] ?: 100
    val waterTarget = preferences[androidx.datastore.preferences.core.intPreferencesKey("water_target_ml")] ?: 2000
    fun toggleMeal(mealId: String) {
        scope.launch {
            val dao = database.mealCompletionDao()
            if (mealId in done) dao.removeCompletion(today, mealId)
            else dao.saveCompletion(MealCompletion(date = today, mealId = mealId))
        }
    }
    MaterialTheme(colorScheme = colors) {
        Scaffold(containerColor = colors.background, bottomBar = {
            NavigationBar(containerColor = colors.surface) {
                listOf("Today", "Plan", "Recipes", "Profile").forEachIndexed { i, label ->
                    val icon = listOf(Icons.Default.Home, Icons.Default.CalendarMonth, Icons.Default.Restaurant, Icons.Default.Person)[i]
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, label) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, indicatorColor = if (dark) Color(0xFF30483A) else Mint))
                }
            }
        }) { p ->
            var dragTotal by remember(tab) { mutableFloatStateOf(0f) }
            Box(Modifier.fillMaxSize().pointerInput(tab) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                    onDragEnd = {
                        if (dragTotal < -80f) tab = (tab + 1).coerceAtMost(3)
                        else if (dragTotal > 80f) tab = (tab - 1).coerceAtLeast(0)
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f }
                )
            }) {
                when(tab) {
                    0 -> Today(p, calorieTarget, proteinTarget, done, customMeals, { meal -> scope.launch { database.customMealDao().insert(meal) } }, { meal -> scope.launch { database.customMealDao().update(meal) } }, ::toggleMeal, { meal -> scope.launch { database.mealCompletionDao().removeCompletion(today,"custom-${meal.id}"); database.customMealDao().delete(meal) } })
                    1 -> Plan(p, database, calorieTarget)
                    2 -> Recipes(p, tier, hostel, database) { selected -> scope.launch { context.ascendPreferences.edit { it[stringPreferencesKey("recipe_tier")] = selected } } }
                    else -> Profile(p, hostel, tier, calorieTarget, proteinTarget, waterTarget, { enabled -> scope.launch { context.ascendPreferences.edit { it[booleanPreferencesKey("hostel_mode")] = enabled } } }, { target -> scope.launch { context.ascendPreferences.edit { it[androidx.datastore.preferences.core.intPreferencesKey("calorie_target")] = target } } }, { target -> scope.launch { context.ascendPreferences.edit { it[androidx.datastore.preferences.core.intPreferencesKey("protein_target")] = target } } }, { target -> scope.launch { context.ascendPreferences.edit { it[androidx.datastore.preferences.core.intPreferencesKey("water_target_ml")] = target } } })
                }
            }
        }
    }
}
