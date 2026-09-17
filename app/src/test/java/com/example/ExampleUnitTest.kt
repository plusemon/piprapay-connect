package com.example

import com.example.parser.MfsSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `parse bKash received payment successfully`() {
        val sms = "You have received Tk 1,500.00 from 01712345678. Ref 01799. Fee Tk 0.00. Balance Tk 6,240.00. TrxID 9ABC123XYZ at 16/09/2026 19:30"
        val parsed = MfsSmsParser.parse("bKash", sms)

        assertNotNull(parsed)
        assertEquals("BKASH", parsed?.provider)
        assertEquals("9ABC123XYZ", parsed?.trxId)
        assertEquals("01712345678", parsed?.senderNumber)
        assertEquals(1500.0, parsed?.amount ?: 0.0, 0.001)
    }

    @Test
    fun `parse Nagad payment format successfully`() {
        val sms = "Amount: Tk 2,000.00, Sender: 01887654321, TxnID: 7XYZ456, Ref: StorePayment. Balance: Tk 12,300.00"
        val parsed = MfsSmsParser.parse("Nagad", sms)

        assertNotNull(parsed)
        assertEquals("NAGAD", parsed?.provider)
        assertEquals("7XYZ456", parsed?.trxId)
        assertEquals("01887654321", parsed?.senderNumber)
        assertEquals(2000.0, parsed?.amount ?: 0.0, 0.001)
    }

    @Test
    fun `parse Rocket cash-in format successfully`() {
        val sms = "Cash In Tk 1,200.00 from 01911223344 successful. Fee Tk 0.00. Balance Tk 4,500.00. TxnId: 9876543210. 16/09/2026"
        val parsed = MfsSmsParser.parse("16216", sms)

        assertNotNull(parsed)
        assertEquals("ROCKET", parsed?.provider)
        assertEquals("9876543210", parsed?.trxId)
        assertEquals(1200.0, parsed?.amount ?: 0.0, 0.001)
    }

    @Test
    fun `parse Upay transaction format successfully`() {
        val sms = "You have received Tk 1,000.00 from 01600112233. TrxID: UP123456. Fee Tk 0.00. New Balance Tk 3,100.00."
        val parsed = MfsSmsParser.parse("Upay", sms)

        assertNotNull(parsed)
        assertEquals("UPAY", parsed?.provider)
        assertEquals("UP123456", parsed?.trxId)
        assertEquals(1000.0, parsed?.amount ?: 0.0, 0.001)
    }

    @Test
    fun `reject bKash OTP verification code alert`() {
        val sms = "Your bKash verification code is 482910. Do not share this OTP with anyone. Ref: Login"
        val parsed = MfsSmsParser.parse("bKash", sms)

        assertNull("OTP messages must be rejected strictly", parsed)
    }

    @Test
    fun `reject message without valid transaction ID`() {
        val sms = "Dear customer, your request has been processed. Thank you."
        val parsed = MfsSmsParser.parse("bKash", sms)

        assertNull("Messages missing TrxID must be rejected", parsed)
    }

    @Test
    fun `merchant settings defaults to not onboarded on first install`() {
        val settings = com.example.data.prefs.MerchantSettings()
        org.junit.Assert.assertFalse(settings.onboardingCompleted)
    }
}
