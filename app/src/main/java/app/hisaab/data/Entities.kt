package app.hisaab.data

import androidx.room.*

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val last4: String? = null,
    val sortOrder: Int = 0, val archived: Boolean = false
)

@Entity(tableName = "buckets")
data class BucketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val monthlyBudgetPaise: Long,
    val rollover: Boolean = false, val isSavings: Boolean = false,
    val sortOrder: Int = 0, val archived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [Index("timestamp"), Index("bucketId"), Index("payeeVpa")]
)
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val payeeName: String?, val payeeVpa: String?, val note: String?,
    val categoryKey: String?, val bucketId: Long?, val accountId: Long?,
    val timestamp: Long, val status: String, val source: String,
    val upiTxnRef: String? = null
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, id") fun all(): kotlinx.coroutines.flow.Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, id") suspend fun allOnce(): List<AccountEntity>
    @Insert suspend fun insert(e: AccountEntity): Long
    @Update suspend fun update(e: AccountEntity)
    @Query("UPDATE accounts SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
}

@Dao
interface BucketDao {
    @Query("SELECT * FROM buckets WHERE archived = 0 ORDER BY sortOrder, id") fun all(): kotlinx.coroutines.flow.Flow<List<BucketEntity>>
    @Query("SELECT * FROM buckets WHERE archived = 0 ORDER BY sortOrder, id") suspend fun allOnce(): List<BucketEntity>
    @Insert suspend fun insert(e: BucketEntity): Long
    @Update suspend fun update(e: BucketEntity)
    @Query("UPDATE buckets SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
    @Query("DELETE FROM buckets") suspend fun deleteAll()
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") fun all(): kotlinx.coroutines.flow.Flow<List<TxnEntity>>
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") suspend fun allOnce(): List<TxnEntity>
    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC") suspend fun inRange(from: Long, to: Long): List<TxnEntity>
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC") suspend fun pending(): List<TxnEntity>
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun byId(id: Long): TxnEntity?
    @Insert suspend fun insert(e: TxnEntity): Long
    @Update suspend fun update(e: TxnEntity)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM transactions") suspend fun deleteAll()
    @Query("SELECT * FROM transactions WHERE payeeVpa = :vpa AND status = 'SUCCESS' ORDER BY timestamp DESC LIMIT 1") suspend fun lastSuccessForPayee(vpa: String): TxnEntity?
}

@Database(entities = [AccountEntity::class, BucketEntity::class, TxnEntity::class], version = 1, exportSchema = false)
abstract class HisaabDb : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun buckets(): BucketDao
    abstract fun txns(): TxnDao
}
