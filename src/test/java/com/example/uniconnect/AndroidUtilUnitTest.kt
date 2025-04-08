package com.example.uniconnect

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.uniconnect.utils.AndroidUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class AndroidUtilUnitTest {
    private val androidUtil = AndroidUtil()

    @Test
    fun testExtractFullNameFromEmail() {
        val email = "john.doe@example.com"
        val fullName = androidUtil.extractFullNameFromEmail(email)
        assertEquals("John Doe", fullName)
    }

    @Test
    fun testValidateTimeFormat_ValidFormat() {
        val timeString = "12:00-15:30"
        assertTrue(androidUtil.validateTimeFormat(timeString))
    }

    @Test
    fun testValidateTimeFormat_InvalidFormat() {
        val timeString = "12:00-1530"
        assertFalse(androidUtil.validateTimeFormat(timeString))
    }

    @Test
    fun testFormatHours() {
        val hours = "9:00-11:30"
        val formattedHours = androidUtil.formatHours(hours)
        assertEquals("9:00-11:30", formattedHours)
    }
}