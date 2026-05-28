package com.tuapp.libreta.data.sync

import com.tuapp.libreta.domain.model.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncConflictResolverTest {

    @Test
    fun `server wins if remote version is higher`() {
        val local = 1L
        val remote = 2L
        assertTrue(SyncConflictResolver.shouldServerWin(local, remote))
    }

    @Test
    fun `server does not win if versions are equal`() {
        val local = 2L
        val remote = 2L
        assertFalse(SyncConflictResolver.shouldServerWin(local, remote))
    }

    @Test
    fun `server does not win if local version is higher`() {
        val local = 3L
        val remote = 2L
        assertFalse(SyncConflictResolver.shouldServerWin(local, remote))
    }

    @Test
    fun `conflict status is correctly returned`() {
        assertEquals(SyncStatus.PENDING_CONFLICT, SyncConflictResolver.resolveStatusOnConflict())
    }
}
