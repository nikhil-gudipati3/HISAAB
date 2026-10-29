package app.hisaab.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HisaabRepo @Inject constructor(val db: HisaabDb) {
    fun accounts(): Flow<List<AccountEntity>> = db.accounts().all()
    fun buckets(): Flow<List<BucketEntity>> = db.buckets().all()
    fun txns(): Flow<List<TxnEntity>> = db.txns().all()
    suspend fun accountsOnce() = db.accounts().allOnce()
    suspend fun bucketsOnce() = db.buckets().allOnce()
    suspend fun txnsOnce() = db.txns().allOnce()
    suspend fun txnsInRange(from: Long, to: Long) = db.txns().inRange(from, to)
    suspend fun pending() = db.txns().pending()
    suspend fun txnById(id: Long) = db.txns().byId(id)
    suspend fun addAccount(e: AccountEntity) = db.accounts().insert(e)
    suspend fun updateAccount(e: AccountEntity) = db.accounts().update(e)
    suspend fun archiveAccount(id: Long) = db.accounts().archive(id)
    suspend fun addBucket(e: BucketEntity) = db.buckets().insert(e)
    suspend fun updateBucket(e: BucketEntity) = db.buckets().update(e)
    suspend fun archiveBucket(id: Long) = db.buckets().archive(id)
    suspend fun addTxn(e: TxnEntity) = db.txns().insert(e)
    suspend fun updateTxn(e: TxnEntity) = db.txns().update(e)
    suspend fun deleteTxn(id: Long) = db.txns().delete(id)
    suspend fun lastSuccessForPayee(vpa: String) = db.txns().lastSuccessForPayee(vpa)
    suspend fun deleteAllTxns() = db.txns().deleteAll()
    suspend fun deleteAllBuckets() = db.buckets().deleteAll()
}
