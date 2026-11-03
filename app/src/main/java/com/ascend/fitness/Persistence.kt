package com.ascend.fitness

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "saved_recipes")
data class SavedRecipe(@PrimaryKey val recipeId: String)

@Dao
interface SavedRecipeDao {
    @Query("SELECT recipeId FROM saved_recipes")
    fun observeSavedRecipeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun save(recipe: SavedRecipe)

    @Query("DELETE FROM saved_recipes WHERE recipeId = :recipeId")
    suspend fun remove(recipeId: String)
}

@Entity(tableName = "custom_meals")
data class CustomMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val time: String,
    val ingredients: String,
    val kcal: Int
)

@Dao
interface CustomMealDao {
    @Query("SELECT * FROM custom_meals WHERE date = :date ORDER BY id")
    fun observeMeals(date: String): Flow<List<CustomMeal>>

    @Insert
    suspend fun insert(meal: CustomMeal)

    @Update
    suspend fun update(meal: CustomMeal)

    @Delete
    suspend fun delete(meal: CustomMeal)
}

@Database(entities = [MealCompletion::class, CustomMeal::class, SavedRecipe::class], version = 3, exportSchema = false)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun mealCompletionDao(): MealCompletionDao
    abstract fun customMealDao(): CustomMealDao\n    abstract fun savedRecipeDao(): SavedRecipeDao

    companion object {
        @Volatile private var INSTANCE: AscendDatabase? = null
        fun getInstance(context: Context): AscendDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AscendDatabase::class.java, "ascend_local.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `custom_meals` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`date` TEXT NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`time` TEXT NOT NULL, " +
            "`ingredients` TEXT NOT NULL, " +
            "`kcal` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {\n    override fun migrate(db: SupportSQLiteDatabase) {\n        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_recipes` (`recipeId` TEXT NOT NULL, PRIMARY KEY(`recipeId`))")\n    }\n}\n