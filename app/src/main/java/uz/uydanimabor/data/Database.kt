package uz.uydanimabor.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** F-02: belgilangan mahsulot. state: 1 = bor, 2 = tez ishlatish kerak. */
@Entity(tableName = "marks")
data class MarkEntity(@PrimaryKey val productId: String, val state: Int)

/** F-10: sevimli taom. */
@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val recipeId: String, val addedAt: Long)

/** F-09: xarid roʻyxati elementi. */
@Entity(tableName = "shopping")
data class ShoppingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val productId: String? = null,
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface MarkDao {
    @Query("SELECT * FROM marks")
    fun observe(): Flow<List<MarkEntity>>

    @Query("SELECT * FROM marks")
    suspend fun all(): List<MarkEntity>

    @Upsert
    suspend fun upsert(mark: MarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(marks: List<MarkEntity>)

    @Query("DELETE FROM marks WHERE productId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM marks")
    suspend fun clear()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observe(): Flow<List<FavoriteEntity>>

    @Upsert
    suspend fun upsert(f: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE recipeId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping ORDER BY checked ASC, createdAt ASC, id ASC")
    fun observe(): Flow<List<ShoppingEntity>>

    @Query("SELECT * FROM shopping")
    suspend fun all(): List<ShoppingEntity>

    @Insert
    suspend fun insert(item: ShoppingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingEntity>)

    @Query("UPDATE shopping SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<ShoppingEntity>) {
        clear()
        insertAll(items)
    }
}

@Database(
    entities = [MarkEntity::class, FavoriteEntity::class, ShoppingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun marks(): MarkDao
    abstract fun favorites(): FavoriteDao
    abstract fun shopping(): ShoppingDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "uydanimabor.db").build()
    }
}
