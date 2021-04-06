package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.shouldNotContainMessage
import io.kotest.matchers.shouldBe
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

    @Test
    fun testCanUseExtensionFunctionTypeUpperBound() = codegen(
        """
            typealias MyBuilder = StringBuilder.() -> Unit
            @Given fun <@Given T : MyBuilder> toString(@Given builder: MyBuilder): String = buildString(builder)
            @Given val myBuilder: MyBuilder = { append("42") }
            fun invoke() = given<String>()
        """
    ) {
        invokeSingleFile() shouldBe "42"
    }
}
