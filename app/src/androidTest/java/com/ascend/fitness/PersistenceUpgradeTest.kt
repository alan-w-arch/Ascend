package com.ascend.fitness

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceUpgradeTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun versionOneDatabaseMigratesToLatestWithoutLosingExistingRows() = runBlocking {
        context.deleteDatabase("ascend_local.db")
        createVersionOneDatabase(context)

        val database = AscendDatabase.getInstance(context)
        val completions = database.mealCompletionDao().getCompletedMealIds("2026-01-15")
        assertEquals(listOf("breakfast"), completions)

        val inserted = CustomMeal(
            date = "2026-01-15",
            title = "Migrated test meal",
            time = "08:00",
            ingredients = "oats",
            kcal = 350,
            protein = 18,
            carbs = 45,
            fat = 9
        )
        database.customMealDao().insert(inserted)
        val stored = database.customMealDao().getMeals("2026-01-15").single()
        assertEquals(inserted.title, stored.title)
        assertEquals(inserted.protein, stored.protein)
        assertEquals(inserted.carbs, stored.carbs)
        assertEquals(inserted.fat, stored.fat)

        // Editing a meal can assign it to another plan date without losing its ID or macros.
        val moved = stored.copy(date = "2026-01-16", title = "Updated meal")
        database.customMealDao().update(moved)
        assertEquals(emptyList<CustomMeal>(), database.customMealDao().getMeals("2026-01-15"))
        assertEquals("Updated meal", database.customMealDao().getMeals("2026-01-16").single().title)

        // Completion is date-scoped and custom meal completion can be toggled independently.
        database.mealCompletionDao().saveCompletion(MealCompletion("2026-01-16", "custom-${stored.id}"))
        assertEquals(listOf("custom-${stored.id}"), database.mealCompletionDao().getCompletedMealIds("2026-01-16"))
        assertEquals(listOf("breakfast"), database.mealCompletionDao().getCompletedMealIds("2026-01-15"))
        database.mealCompletionDao().removeCompletion("2026-01-16", "custom-${stored.id}")
        database.customMealDao().delete(moved)
        assertEquals(emptyList<CustomMeal>(), database.customMealDao().getMeals("2026-01-16"))

        // Saved recipes are durable independently of date-specific meal planning.
        database.savedRecipeDao().save(SavedRecipe("Banana oats bowl"))
        assertEquals(listOf("Banana oats bowl"), database.savedRecipeDao().observeSavedRecipeIds().first())
        database.close()

        val reopenedDatabase = AscendDatabase.getInstance(context)
        assertEquals(listOf("Banana oats bowl"), reopenedDatabase.savedRecipeDao().observeSavedRecipeIds().first())
        reopenedDatabase.savedRecipeDao().remove("Banana oats bowl")
        reopenedDatabase.close()
    }

    @Test
    fun dataStorePreferencesRemainReadableAfterReopeningTheStore() = runBlocking {
        val file = context.preferencesDataStoreFile("persistence_upgrade_test")
        file.delete()
        val firstJob = SupervisorJob()
        val firstScope = CoroutineScope(firstJob + Dispatchers.IO)
        val firstStore = PreferenceDataStoreFactory.create(
            scope = firstScope,
            produceFile = { file }
        )
        firstStore.edit {
            it[booleanPreferencesKey("hostel_mode")] = false
            it[stringPreferencesKey("recipe_tier")] = "Standard"
            it[intPreferencesKey("calorie_target")] = 2200
        }
        firstJob.cancel()

        val secondJob = SupervisorJob()
        val secondScope = CoroutineScope(secondJob + Dispatchers.IO)
        val reopenedStore = PreferenceDataStoreFactory.create(
            scope = secondScope,
            produceFile = { file }
        )
        val reopened = reopenedStore.data.first()
        assertEquals(false, reopened[booleanPreferencesKey("hostel_mode")])
        assertEquals("Standard", reopened[stringPreferencesKey("recipe_tier")])
        assertEquals(2200, reopened[intPreferencesKey("calorie_target")])
        secondJob.cancel()
    }

    private fun createVersionOneDatabase(context: Context) {
        val helper = object : SQLiteOpenHelper(context, "ascend_local.db", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS meal_completions (" +
                        "date TEXT NOT NULL, mealId TEXT NOT NULL, " +
                        "PRIMARY KEY(date, mealId))"
                )
                db.execSQL(
                    "INSERT INTO meal_completions(date, mealId) VALUES " +
                        "('2026-01-15', 'breakfast')"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
        helper.close()
    }
}
