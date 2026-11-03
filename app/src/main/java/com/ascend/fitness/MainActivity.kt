package com.ascend.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

private val Green = Color(0xFF287A55)
private val Mint = Color(0xFFE7F3EB)
private val Canvas = Color(0xFFF6F8F5)
private val Ink = Color(0xFF17231D)
private val Muted = Color(0xFF6C786F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AscendApp() }
    }
}

@Composable
fun AscendApp() {
    var tab by remember { mutableIntStateOf(0) }
    var hostel by remember { mutableStateOf(true) }
    var tier by remember { mutableStateOf("Budget") }
    var done by remember { mutableStateOf(setOf<String>()) }
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
                0 -> Today(p, done) { done = done + it }
                1 -> Plan(p, done) { done = done + it }
                2 -> Recipes(p, tier) { tier = it }
                else -> Profile(p, hostel) { hostel = it }
            }
        }
    }
}

private data class Meal(val name:String, val time:String, val title:String, val ingredients:String, val kcal:Int, val emoji:String)
private val meals = listOf(
    Meal("Breakfast","8:00 AM","Banana oats bowl","Oats · banana · milk",380,"🥣"),
    Meal("Lunch","1:00 PM","Chickpea rice bowl","Chickpeas · rice · cucumber",520,"🍛"),
    Meal("Snack","4:30 PM","Peanut banana bites","Banana · peanuts",180,"🍌"),
    Meal("Dinner","8:00 PM","Moong dal khichdi","Moong dal · rice · spices",420,"🍲")
)

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
                Text("${meal.kcal} kcal",color=Green,fontSize=12.sp,fontWeight=FontWeight.Bold)
            }
            IconButton(onClick=onCheck) { Icon(if(checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,"Mark complete",tint=if(checked) Green else Muted) }
        }
    }
}
@Composable private fun Today(p:PaddingValues, done:Set<String>, onDone:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Good morning","Your plan for today") }
        item {
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Green)) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("DAILY TARGET",color=Color.White.copy(alpha=.8f),fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Row(verticalAlignment=Alignment.Bottom) { Text("1,240",color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold); Text(" / 2,000 kcal",color=Color.White.copy(alpha=.8f),modifier=Modifier.padding(bottom=6.dp)) }
                    LinearProgressIndicator(progress={.62f},modifier=Modifier.fillMaxWidth().height(8.dp),color=Color.White,trackColor=Color.White.copy(alpha=.25f))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Macro("Protein","68 g"); Macro("Carbs","142 g"); Macro("Fat","38 g") }
                }
            }
        }
        item { Section("Today's meals","4 meals") }
        items(meals) { MealCard(it,done.contains(it.name)){onDone(it.name)} }
        item { Text("Sample nutrition targets only; consult a qualified professional for individualized medical nutrition advice.",color=Muted,fontSize=11.sp) }
    }
}
@Composable private fun Plan(p:PaddingValues,done:Set<String>,onDone:(String)->Unit) {
    var day by remember { mutableIntStateOf(3) }
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Meal plan","November 2026 · Plan ahead") }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { listOf("M","T","W","T","F","S","S").forEach { Text(it,color=Muted,modifier=Modifier.width(32.dp)) } }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        (1..7).forEach { n -> Box(Modifier.size(32.dp).background(if(n==day) Green else Color.Transparent,CircleShape).clickable{day=n},contentAlignment=Alignment.Center) { Text("$n",color=if(n==day) Color.White else Ink) } }
                    }
                    Text("Select a date to preview meals. Calendar sync and reminders are planned for a future release.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
                }
            }
        }
        item { Section("November $day","2,000 kcal target") }
        items(meals) { MealCard(it,done.contains(it.name)){onDone(it.name)} }
    }
}
private data class Recipe(val title:String,val timing:String,val kcal:Int,val cost:Int,val tags:String,val emoji:String)
private val recipes=listOf(
    Recipe("Banana oats bowl","5 min · No cook",380,32,"Hostel-friendly · Breakfast","🥣"),
    Recipe("Chickpea rice bowl","15 min · One pot",520,48,"High protein · Lunch","🍛"),
    Recipe("Peanut banana bites","3 min · No cook",180,18,"Snack · No cook","🍌"),
    Recipe("Moong dal khichdi","20 min · One pot",420,36,"Comfort food · Dinner","🍲"),
    Recipe("Paneer veggie wrap","12 min · Pan",460,72,"Vegetarian · Lunch","🌯")
)
@Composable private fun Recipes(p:PaddingValues,tier:String,onTier:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Recipe library","Simple, student-friendly meals") }
        item {
            Text("Your budget",color=Ink,fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=8.dp)) {
                listOf("Budget","Standard","Premium").forEach { FilterChip(selected=tier==it,onClick={onTier(it)},label={Text(it)}) }
            }
        }
        items(recipes.filter { tier!="Budget" || it.cost<=50 }) { r ->
            Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(Mint,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){Text(r.emoji,fontSize=26.sp)}
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(r.title,color=Ink,fontWeight=FontWeight.SemiBold)
                        Text(r.timing,color=Muted,fontSize=12.sp)
                        Text(r.tags,color=Green,fontSize=11.sp)
                        Text("${r.kcal} kcal · ~₹${r.cost}/serving",color=Muted,fontSize=12.sp)
                    }
                    Icon(Icons.Default.BookmarkBorder,"Save recipe",tint=Green)
                }
            }
        }
        item { Text("Costs are illustrative estimates, not live local prices.",color=Muted,fontSize=11.sp) }
    }
}
@Composable private fun Profile(p:PaddingValues,hostel:Boolean,onHostel:(Boolean)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Your profile","Preferences for a plan that fits your life") }
        item {
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Personal details",color=Ink,fontWeight=FontWeight.Bold)
                    ProfileRow("Goal","Healthy weight management")
                    ProfileRow("Daily energy target","2,000 kcal")
                    ProfileRow("Diet","Vegetarian")
                    ProfileRow("Budget","Budget · ₹50/meal")
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
        item { Text("Profile preferences are local demo state. Membership and expert booking are not connected yet.",color=Muted,fontSize=11.sp) }
    }
}
@Composable private fun ProfileRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,color=Muted); Text(value,color=Ink,fontWeight=FontWeight.Medium) }
}
