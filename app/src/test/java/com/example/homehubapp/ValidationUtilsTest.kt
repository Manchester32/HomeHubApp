package com.example.homehubapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validEmail_isAccepted() {
        assertTrue(ValidationUtils.isValidEmail("lwethu@example.com"))
    }

    @Test
    fun emailWithoutAt_isRejected() {
        assertFalse(ValidationUtils.isValidEmail("lwethuexample.com"))
    }

    @Test
    fun emailWithoutDomain_isRejected() {
        assertFalse(ValidationUtils.isValidEmail("lwethu@"))
    }

    @Test
    fun strongPassword_isAccepted() {
        assertTrue(ValidationUtils.isValidPassword("Home#123"))
    }

    @Test
    fun passwordTooShort_isRejected() {
        assertFalse(ValidationUtils.isValidPassword("Ab1#"))
    }

    @Test
    fun passwordWithoutSymbol_isRejected() {
        assertFalse(ValidationUtils.isValidPassword("Home1234"))
    }

    @Test
    fun passwordWithoutUppercase_isRejected() {
        assertFalse(ValidationUtils.isValidPassword("home#123"))
    }
}