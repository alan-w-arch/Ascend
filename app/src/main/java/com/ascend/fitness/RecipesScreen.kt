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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.launch
import java.time.LocalDate


data class Recipe(val title:String,val timing:String,val kcal:Int,val cost:Int,val tags:String,val emoji:String,val protein:Int,val carbs:Int,val fat:Int)
val recipes=listOf(
    Recipe("Banana oats bowl","5 min · No cook",380,32,"Hostel-friendly · Breakfast","🥣",14,62,9),
    Recipe("Chickpea rice bowl","15 min · One pot",520,48,"High protein · Lunch","🍛",18,88,8),
    Recipe("Peanut banana bites","3 min · No cook",180,18,"Snack · No cook","🍌",5,27,8),
    Recipe("Moong dal khichdi","20 min · One pot",420,36,"Comfort food · Dinner","🍲",17,72,7),
    Recipe("Paneer veggie wrap","12 min · Pan",460,72,"Vegetarian · Lunch","🌯",22,48,18)
)
@Composable fun Recipes(p:PaddingValues,tier:String,hostel:Boolean,database:AscendDatabase,onTier:(String)->Unit) {
    val scope = rememberCoroutineScope()
    val savedIds by database.savedRecipeDao().observeSavedRecipeIds().collectAsState(initial=emptyList())
    var savedOnly by remember { mutableStateOf(false) }
    var highProteinOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Recommended") }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    val maxCost = when(tier) { "Budget" -> 50; "Standard" -> 75; else -> Int.MAX_VALUE }
    val query = searchQuery.trim()
    val filteredRecipes = recipes.filter { it.cost <= maxCost && (!savedOnly || it.title in savedIds) && (!highProteinOnly || it.protein >= 20) && (!hostel || it.timing.contains("No cook") || it.timing.contains("One pot")) && (query.isBlank() || query.split(Regex("\\s+")).filter { term -> term.isNotBlank() }.all { term -> it.title.contains(term, ignoreCase=true) || it.tags.contains(term, ignoreCase=true) || it.timing.contains(term, ignoreCase=true) }) }
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
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=savedOnly,onClick={savedOnly=!savedOnly},label={Text(if(savedOnly) "Saved only" else "Saved recipes (${savedIds.size})")})
            FilterChip(selected=highProteinOnly,onClick={highProteinOnly=!highProteinOnly},label={Text("High protein · 20g+")})
        } }
        item { if(query.isNotBlank() || savedOnly || highProteinOnly || sortBy != "Recommended") TextButton(onClick={searchQuery="";savedOnly=false;highProteinOnly=false;sortBy="Recommended"}) { Text("Reset filters") } }
        item { if(hostel) Text("Hostel mode: showing no-cook and one-pot options.",color=Muted,fontSize=12.sp) }
        items(visibleRecipes,key={it.title}) { r ->
            Card(modifier=Modifier.clickable(role=Role.Button,onClick={selectedRecipe=r}),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(Mint,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){Text(r.emoji,fontSize=26.sp)}
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(r.title,color=Ink,fontWeight=FontWeight.SemiBold)
                        Text(r.timing,color=Muted,fontSize=12.sp)
                        Text(r.tags,color=Green,fontSize=11.sp)
                        Text("${r.kcal} kcal · ~₹${r.cost}/serving",color=Muted,fontSize=12.sp)
                        Text("P ${r.protein}g · C ${r.carbs}g · F ${r.fat}g",color=Green,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                        Text("Sample nutrition estimate · not verified",color=Muted,fontSize=10.sp)
                        Text("View recipe",color=Green,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                    }
                    IconButton(onClick={scope.launch { if(r.title in savedIds) database.savedRecipeDao().remove(r.title) else database.savedRecipeDao().save(SavedRecipe(r.title)) }}) {
                        Icon(if(r.title in savedIds) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,if(r.title in savedIds) "Remove saved recipe" else "Save recipe",tint=Green)
                    }
                }
            }
        }
        item { if(visibleRecipes.isEmpty()) Column(verticalArrangement=Arrangement.spacedBy(6.dp)) { Text("No recipes match your current filters.",color=Muted); TextButton(onClick={searchQuery="";savedOnly=false;highProteinOnly=false;sortBy="Recommended"}) { Text("Clear search and filters") } } }
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
                Text("Sample estimate · not verified against exact ingredients or portions",color=Muted,fontSize=11.sp)
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
