package com.ascend.fitness

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

val Context.ascendPreferences by preferencesDataStore(name = "ascend_preferences")

@Entity(tableName = "meal_completions", primaryKeys = ["date", "mealId"])
data class MealCompletion(val date: String, val mealId: String)

@Dao
interface MealCompletionDao {
    @Query("SELECT mealId FROM meal_completions WHERE date = :date")
    fun observeCompletedMealIds(date: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletion(completion: MealCompletion)

    @Query("DELETE FROM meal_completions WHERE date = :date AND mealId = :mealId")
    suspend fun removeCompletion(date: String, mealId: String)
}

@Database(entities = [MealCompletion::class], version = 1, exportSchema = false)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun mealCompletionDao(): MealCompletionDao

    companion object {
        @Volatile private var INSTANCE: AscendDatabase? = null
        fun getInstance(context: Context): AscendDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AscendDatabase::class.java, "ascend_local.db")
                .build().also { INSTANCE = it }
        }
    }
}
