package com.peanut.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SupplierNamingTest {
    @Test
    fun nextSupplierNameContinuesSequence() {
        val entries = listOf(
            LedgerEntry(supplierName = "供货商1"),
            LedgerEntry(supplierName = "供货商2")
        )
        assertEquals("供货商3", nextSupplierName(entries))
    }

    @Test
    fun nextSupplierNameUnderstandsLegacyAutoNames() {
        val legacy = listOf(
            LedgerEntry(supplierName = "户1"),
            LedgerEntry(supplierName = "2")
        )
        assertEquals("供货商3", nextSupplierName(legacy))
    }

    @Test
    fun namedSupplierDoesNotConsumeASequence() {
        assertEquals(
            "供货商1",
            nextSupplierName(listOf(LedgerEntry(supplierName = "张三")))
        )
    }

    @Test
    fun partyNameUsesTheRightSideForTab() {
        val entry = LedgerEntry(supplierName = "供货商1", buyerName = "油厂")
        assertEquals("供货商1", entry.partyName(LedgerTab.PURCHASE))
        assertEquals("油厂", entry.partyName(LedgerTab.SALE))
    }
}
