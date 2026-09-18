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
    val senderKey: String = provider.lowercase(),
    val senderNumber: String,
    val amount: Double,
    val balance: Double? = null, // Extracted post-transaction wallet balance for pp_balance_verification
    val currency: String = "BDT",
    val type: String = "received", // "received", "payment", "cash_in"
    val simSlot: Int = 1, // SIM 1 or SIM 2
    val rawMessage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val retryCount: Int = 0,
    val syncErrorMessage: String? = null
)
