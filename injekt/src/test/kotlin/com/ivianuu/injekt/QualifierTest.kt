package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import org.junit.Test

class QualifierTest {

    @Test
    fun doesOrderingMatter() {
        val left = TestQualifier1 + TestQualifier2 + TestQualifier3
        val right = TestQualifier2 + TestQualifier1 + TestQualifier3
        assertEquals(left, right)
    }

}