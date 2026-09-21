package com.example.homehubapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RequestStatusHelperTest {

    @Test
    fun pending_movesToInProgress() {
        assertEquals("In Progress", RequestStatusHelper.nextStatus("Pending"))
    }

    @Test
    fun inProgress_movesToCompleted() {
        assertEquals("Completed", RequestStatusHelper.nextStatus("In Progress"))
    }

    @Test
    fun completed_hasNoNextStatus() {
        assertNull(RequestStatusHelper.nextStatus("Completed"))
    }

    @Test
    fun completedPrototypeAmount_is450() {
        assertEquals(450.0, RequestStatusHelper.amountWhenCompleted(), 0.0)
    }
}
