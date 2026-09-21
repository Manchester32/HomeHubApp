package com.example.homehubapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validEmail_returnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"))
    }

    @Test
    fun invalidEmail_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("userexample.com"))
    }

    @Test
    fun strongPassword_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("HomeHub@123"))
    }

    @Test
    fun passwordWithoutUppercase_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("homehub@123"))
    }

    @Test
    fun passwordWithoutLowercase_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("HOMEHUB@123"))
    }

    @Test
    fun passwordWithoutNumber_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("HomeHub@abc"))
    }

    @Test
    fun passwordWithoutSymbol_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("HomeHub123"))
    }

    @Test
    fun passwordShorterThanSixCharacters_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("Aa@12"))
    }
}
