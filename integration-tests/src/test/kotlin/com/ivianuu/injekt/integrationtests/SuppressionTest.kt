package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.shouldNotContainMessage
import org.junit.Test

class SuppressionTest {

    @Test
    fun testDoesNotWarnFinalUpperBound() = codegen(
        """
            fun <T : Int> func() {
            }
        """
    ) {
        shouldNotContainMessage("'Int' is a final type, and thus a value of the type parameter is predetermined")
    }

}