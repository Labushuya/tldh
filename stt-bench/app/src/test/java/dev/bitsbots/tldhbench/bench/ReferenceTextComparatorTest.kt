package dev.bitsbots.tldhbench.bench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceTextComparatorTest {
    @Test fun blankReferenceDisablesComparison() {
        assertNull(ReferenceTextComparator.compare("   ", "hallo welt"))
    }

    @Test fun identicalTextHasZeroWerAndCer() {
        val result = requireNotNull(ReferenceTextComparator.compare("Hallo Welt!", "hallo welt"))
        assertEquals(0.0, result.werPercent, 0.0001)
        assertEquals(0.0, result.cerPercent, 0.0001)
        assertEquals(0, result.wordDistance)
    }

    @Test fun detectsMissingNegation() {
        val result = requireNotNull(ReferenceTextComparator.compare("Das ist nicht richtig", "das ist richtig"))
        assertEquals(1, result.wordDeletions)
        assertEquals(25.0, result.werPercent, 0.0001)
    }
}
