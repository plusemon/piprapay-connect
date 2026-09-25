package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(trx: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE is_synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedMessages(limit: Int = 50): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE is_synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET is_synced = 1, syncErrorMessage = null WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("UPDATE transactions SET is_synced = 1, syncErrorMessage = null WHERE trxId = :trxId")
    suspend fun updateStatus(trxId: String)

    @Query("UPDATE transactions SET sync_attempts = sync_attempts + 1, syncErrorMessage = :errorMessage WHERE id IN (:ids)")
    suspend fun incrementSyncAttempts(ids: List<Long>, errorMessage: String?)

    @Query("UPDATE transactions SET sync_attempts = :retryCount, syncErrorMessage = :errorMessage WHERE trxId = :trxId")
    suspend fun updateSyncFailure(trxId: String, retryCount: Int, errorMessage: String?)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int = 15): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE trxId = :trxId LIMIT 1")
    suspend fun getTransactionByTrxId(trxId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions WHERE trxId = :trxId")
    suspend fun deleteTransaction(trxId: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_synced = 1")
    fun getSyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE is_synced = 0")
    fun getPendingCountFlow(): Flow<Int>
}
