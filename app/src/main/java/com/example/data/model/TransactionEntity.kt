package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["trxId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val trxId: String,
    val provider: String, // "BKASH", "NAGAD", "ROCKET", "UPAY"
    val senderNumber: String,
    val amount: Double,
    val rawMessage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val retryCount: Int = 0,
    val syncErrorMessage: String? = null
)
