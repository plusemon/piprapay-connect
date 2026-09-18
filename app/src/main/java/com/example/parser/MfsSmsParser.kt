package com.example.parser

import java.util.Locale

data class ParsedMfsTransaction(
    val provider: String,          // "BKASH", "NAGAD", "ROCKET", "UPAY"
    val senderKey: String,         // "bkash", "nagad", "rocket", "upay"
    val trxId: String,
    val senderNumber: String,
    val amount: Double,
    val balance: Double? = null,   // Remaining wallet balance from SMS (for pp_balance_verification)
    val currency: String = "BDT",
    val type: String = "received", // "received", "payment", "cash_in"
    val rawMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

object MfsSmsParser {

    // Words indicating OTP / security / non-transaction alerts
    private val REJECT_KEYWORDS = listOf(
        "otp",
        "verification code",
        "security code",
        "pin reset",
        "password reset",
        "do not share",
        "never share",
        "login code",
        "secret code",
        "temporary code",
        "authorization code"
    )

    // bKash: "You have received Tk 1,500.00 from 017XXXXXXXX... Fee Tk 0.00. Balance Tk 14,250.00. TrxID 9ABC123XYZ"
    private val BKASH_AMOUNT_REGEX = Regex(
        """(?:received|received\s+deposit|payment\s+received)\s+(?:Tk\.?|BDT)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val BKASH_SENDER_REGEX = Regex(
        """from\s+([0-9A-Za-z*+\-_]{6,15})""",
        RegexOption.IGNORE_CASE
    )
    private val BKASH_TRX_REGEX = Regex(
        """TrxID\s*[:\s]?\s*([A-Za-z0-9]+)""",
        RegexOption.IGNORE_CASE
    )
    private val BKASH_BALANCE_REGEX = Regex(
        """(?:Balance|Current\s*Balance)\s*[:\s]*\s*(?:(?:Tk\.?|BDT)\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Nagad: "Amount: Tk 2,000.00, Sender: 018XXXXXXXX, TxnID: 7XYZ456, Balance: Tk 12,300.00"
    private val NAGAD_AMOUNT_REGEX = Regex(
        """(?:Amount\s*:\s*(?:Tk\.?|BDT)?\s*|received\s+(?:Tk\.?|BDT)?\s*)([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val NAGAD_SENDER_REGEX = Regex(
        """(?:Sender\s*:\s*|from\s+)([0-9A-Za-z*+\-_]{6,15})""",
        RegexOption.IGNORE_CASE
    )
    private val NAGAD_TRX_REGEX = Regex(
        """(?:TxnID|TrxID)\s*[:\s]?\s*([A-Za-z0-9]+)""",
        RegexOption.IGNORE_CASE
    )
    private val NAGAD_BALANCE_REGEX = Regex(
        """(?:Balance|Current\s*Balance)\s*[:\s]*\s*(?:(?:Tk\.?|BDT)\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Rocket (DBBL 16216): "Cash In Tk 1,200.00 from 01XXXXXXXXX successful... Balance Tk 4,500.00. TxnId: 1234567890"
    private val ROCKET_AMOUNT_REGEX = Regex(
        """(?:Cash\s*In\s+(?:Tk\.?|BDT)?\s*|received\s+(?:Tk\.?|BDT)?\s*)([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val ROCKET_SENDER_REGEX = Regex(
        """from\s+(?:A/C\s*)?([0-9A-Za-z*+\-_]{6,18})""",
        RegexOption.IGNORE_CASE
    )
    private val ROCKET_TRX_REGEX = Regex(
        """(?:TxnId|TxnID|TrxID)\s*[:\s]?\s*([A-Za-z0-9]+)""",
        RegexOption.IGNORE_CASE
    )
    private val ROCKET_BALANCE_REGEX = Regex(
        """(?:Balance|A\/C\s*Balance)\s*[:\s]*\s*(?:(?:Tk\.?|BDT)\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Upay: "You have received Tk 1,000.00 from 01XXXXXXXXX. TrxID: UP123456. Balance Tk 5,400.00"
    private val UPAY_AMOUNT_REGEX = Regex(
        """(?:received\s+(?:Tk\.?|BDT)?\s*|Cash\s*In\s+of\s+(?:Tk\.?|BDT)?\s*)([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val UPAY_TRX_REGEX = Regex(
        """(?:TrxID|TxnID)\s*[:\s]?\s*([A-Za-z0-9]+)""",
        RegexOption.IGNORE_CASE
    )
    private val UPAY_BALANCE_REGEX = Regex(
        """(?:Balance|New\s*Balance)\s*[:\s]*\s*(?:(?:Tk\.?|BDT)\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // General Fallback for Bangladeshi MFS
    private val GENERIC_TRX_REGEX = Regex(
        """\b(?:TrxID|TxnID|TxnId|Transaction\s*ID|Ref\s*ID)\s*[:\s#]?\s*([A-Za-z0-9]{5,24})\b""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_AMOUNT_REGEX = Regex(
        """(?:Tk\.?|BDT)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_SENDER_REGEX = Regex(
        """(?:from|sender\s*:?)\s*([0-9A-Za-z*+\-_]{6,15})""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_BALANCE_REGEX = Regex(
        """\b(?:Balance|Bal|Current\s*Balance|Rem\s*Balance)\s*[:\s]*\s*(?:(?:Tk\.?|BDT)\s*)?([0-9,]+(?:\.[0-9]{1,2})?)\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses an incoming SMS message.
     * Returns a valid [ParsedMfsTransaction] or null if the message is invalid,
     * an OTP, or lacks a verifiable transaction ID / amount.
     */
    fun parse(senderAddress: String?, messageBody: String?, smsTimestamp: Long = System.currentTimeMillis()): ParsedMfsTransaction? {
        if (messageBody.isNullOrBlank()) return null

        val lowerBody = messageBody.lowercase(Locale.ROOT)

        // 1. Strictly reject OTPs and security verification alerts
        if (REJECT_KEYWORDS.any { lowerBody.contains(it) }) {
            return null
        }

        // 2. Identify MFS Provider
        val provider = detectProvider(senderAddress, messageBody)

        // 3. Extract according to provider logic with fallback
        return when (provider) {
            "BKASH" -> parseBkash(messageBody, senderAddress, smsTimestamp)
                ?: parseFallback(messageBody, "BKASH", senderAddress, smsTimestamp)
            "NAGAD" -> parseNagad(messageBody, senderAddress, smsTimestamp)
                ?: parseFallback(messageBody, "NAGAD", senderAddress, smsTimestamp)
            "ROCKET" -> parseRocket(messageBody, senderAddress, smsTimestamp)
                ?: parseFallback(messageBody, "ROCKET", senderAddress, smsTimestamp)
            "UPAY" -> parseUpay(messageBody, senderAddress, smsTimestamp)
                ?: parseFallback(messageBody, "UPAY", senderAddress, smsTimestamp)
            else -> parseFallback(messageBody, "MFS", senderAddress, smsTimestamp)
        }
    }

    private fun detectProvider(senderAddress: String?, messageBody: String): String {
        val sender = senderAddress?.lowercase(Locale.ROOT) ?: ""
        val body = messageBody.lowercase(Locale.ROOT)

        return when {
            sender.contains("bkash") || body.contains("bkash") -> "BKASH"
            sender.contains("nagad") || body.contains("nagad") -> "NAGAD"
            sender.contains("rocket") || sender.contains("16216") || body.contains("rocket") || body.contains("dbbl") -> "ROCKET"
            sender.contains("upay") || body.contains("upay") -> "UPAY"
            else -> "UNKNOWN"
        }
    }

    private fun detectType(messageBody: String): String {
        val lower = messageBody.lowercase(Locale.ROOT)
        return when {
            lower.contains("cash in") -> "cash_in"
            lower.contains("payment received") || lower.contains("payment") -> "payment"
            else -> "received"
        }
    }

    private fun parseBkash(body: String, senderAddress: String?, timestamp: Long): ParsedMfsTransaction? {
        val trxMatch = BKASH_TRX_REGEX.find(body) ?: return null
        val trxId = trxMatch.groupValues[1].trim()
        if (trxId.length < 4) return null

        val amountMatch = BKASH_AMOUNT_REGEX.find(body) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        if (amount <= 0.0) return null

        val senderMatch = BKASH_SENDER_REGEX.find(body)
        val senderNumber = cleanSender(senderMatch?.groupValues?.get(1) ?: senderAddress ?: "UNKNOWN")

        val balanceMatch = BKASH_BALANCE_REGEX.find(body)
        val balance = balanceMatch?.groupValues?.get(1)?.let { parseAmount(it) }

        return ParsedMfsTransaction(
            provider = "BKASH",
            senderKey = "bkash",
            trxId = trxId,
            senderNumber = senderNumber,
            amount = amount,
            balance = balance,
            currency = "BDT",
            type = detectType(body),
            rawMessage = body,
            timestamp = timestamp
        )
    }

    private fun parseNagad(body: String, senderAddress: String?, timestamp: Long): ParsedMfsTransaction? {
        val trxMatch = NAGAD_TRX_REGEX.find(body) ?: return null
        val trxId = trxMatch.groupValues[1].trim()
        if (trxId.length < 4) return null

        val amountMatch = NAGAD_AMOUNT_REGEX.find(body) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        if (amount <= 0.0) return null

        val senderMatch = NAGAD_SENDER_REGEX.find(body)
        val senderNumber = cleanSender(senderMatch?.groupValues?.get(1) ?: senderAddress ?: "UNKNOWN")

        val balanceMatch = NAGAD_BALANCE_REGEX.find(body)
        val balance = balanceMatch?.groupValues?.get(1)?.let { parseAmount(it) }

        return ParsedMfsTransaction(
            provider = "NAGAD",
            senderKey = "nagad",
            trxId = trxId,
            senderNumber = senderNumber,
            amount = amount,
            balance = balance,
            currency = "BDT",
            type = detectType(body),
            rawMessage = body,
            timestamp = timestamp
        )
    }

    private fun parseRocket(body: String, senderAddress: String?, timestamp: Long): ParsedMfsTransaction? {
        val trxMatch = ROCKET_TRX_REGEX.find(body) ?: return null
        val trxId = trxMatch.groupValues[1].trim()
        if (trxId.length < 4) return null

        val amountMatch = ROCKET_AMOUNT_REGEX.find(body) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        if (amount <= 0.0) return null

        val senderMatch = ROCKET_SENDER_REGEX.find(body)
        val senderNumber = cleanSender(senderMatch?.groupValues?.get(1) ?: senderAddress ?: "UNKNOWN")

        val balanceMatch = ROCKET_BALANCE_REGEX.find(body)
        val balance = balanceMatch?.groupValues?.get(1)?.let { parseAmount(it) }

        return ParsedMfsTransaction(
            provider = "ROCKET",
            senderKey = "rocket",
            trxId = trxId,
            senderNumber = senderNumber,
            amount = amount,
            balance = balance,
            currency = "BDT",
            type = detectType(body),
            rawMessage = body,
            timestamp = timestamp
        )
    }

    private fun parseUpay(body: String, senderAddress: String?, timestamp: Long): ParsedMfsTransaction? {
        val trxMatch = UPAY_TRX_REGEX.find(body) ?: return null
        val trxId = trxMatch.groupValues[1].trim()
        if (trxId.length < 4) return null

        val amountMatch = UPAY_AMOUNT_REGEX.find(body) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        if (amount <= 0.0) return null

        val senderMatch = GENERIC_SENDER_REGEX.find(body)
        val senderNumber = cleanSender(senderMatch?.groupValues?.get(1) ?: senderAddress ?: "UNKNOWN")

        val balanceMatch = UPAY_BALANCE_REGEX.find(body)
        val balance = balanceMatch?.groupValues?.get(1)?.let { parseAmount(it) }

        return ParsedMfsTransaction(
            provider = "UPAY",
            senderKey = "upay",
            trxId = trxId,
            senderNumber = senderNumber,
            amount = amount,
            balance = balance,
            currency = "BDT",
            type = detectType(body),
            rawMessage = body,
            timestamp = timestamp
        )
    }

    private fun parseFallback(body: String, detectedProvider: String, senderAddress: String?, timestamp: Long): ParsedMfsTransaction? {
        val trxMatch = GENERIC_TRX_REGEX.find(body) ?: return null
        val trxId = trxMatch.groupValues[1].trim()
        if (trxId.length < 4) return null

        val amountMatch = GENERIC_AMOUNT_REGEX.find(body) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        if (amount <= 0.0) return null

        val senderMatch = GENERIC_SENDER_REGEX.find(body)
        val senderNumber = cleanSender(senderMatch?.groupValues?.get(1) ?: senderAddress ?: "UNKNOWN")

        val balanceMatch = GENERIC_BALANCE_REGEX.find(body)
        val balance = balanceMatch?.groupValues?.get(1)?.let { parseAmount(it) }

        val finalProvider = if (detectedProvider == "UNKNOWN") "MFS" else detectedProvider
        val senderKey = finalProvider.lowercase(Locale.ROOT)

        return ParsedMfsTransaction(
            provider = finalProvider,
            senderKey = senderKey,
            trxId = trxId,
            senderNumber = senderNumber,
            amount = amount,
            balance = balance,
            currency = "BDT",
            type = detectType(body),
            rawMessage = body,
            timestamp = timestamp
        )
    }

    private fun parseAmount(raw: String): Double? {
        return try {
            raw.replace(",", "").toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanSender(raw: String): String {
        val trimmed = raw.trim()
        return trimmed.trimEnd('.', ',', ';', ':')
    }
}
