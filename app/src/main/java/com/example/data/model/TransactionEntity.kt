package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SMS item entity stored in Room database.
 * Matches required schema: (id, sender, message, sim_slot, timestamp, is_synced, sync_attempts)
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sender: String,
    val message: String,
    val sim_slot: String = "1",
    val timestamp: Long = System.currentTimeMillis(),
    val is_synced: Boolean = false,
    val sync_attempts: Int = 0,
    // Extracted telemetry & parser fields
    val trxId: String = "",
    val provider: String = sender,
    val senderKey: String = sender.lowercase(),
    val senderNumber: String = "",
    val amount: Double = 0.0,
    val balance: Double? = null,
    val currency: String = "BDT",
    val type: String = "received",
    val syncErrorMessage: String? = null
) {
    val syncStatus: String
        get() = if (is_synced) "SYNCED" else if (sync_attempts > 0 && syncErrorMessage != null) "FAILED" else "PENDING"

    val retryCount: Int
        get() = sync_attempts

    val rawMessage: String
        get() = message

    val simSlot: Int
        get() = sim_slot.toIntOrNull() ?: 1
}
