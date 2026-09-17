package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(trx: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = :status WHERE trxId = :trxId")
    suspend fun updateStatus(trxId: String, status: String)

    @Query("UPDATE transactions SET syncStatus = :status, retryCount = :retryCount, syncErrorMessage = :errorMessage WHERE trxId = :trxId")
    suspend fun updateSyncFailure(trxId: String, status: String, retryCount: Int, errorMessage: String?)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE trxId = :trxId LIMIT 1")
    suspend fun getTransactionById(trxId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE trxId = :trxId")
    suspend fun deleteTransaction(trxId: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE syncStatus = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
}
