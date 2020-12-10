package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class GivenDeclarationCheckTest {

    @Test
    fun testNonGivenValueParameterOnGivenDeclaration() = codegen(
        """
            @Given fun bar(foo: Foo) = Bar(foo)
        """
    ) {
        assertCompileError("Non @Given value parameter")
    }

    @Test
    fun testGivenValueParameterWithoutDefault() = codegen(
        """
            fun bar(foo: @Given Foo) = Bar(foo)
        """
    ) {
        assertCompileError("@Given parameter must have have default")
    }

    @Test
    fun testGivenWithExtensionReceiver() = codegen(
        """
            @Given fun Foo.bar() = Bar(this)
        """
    ) {
        assertCompileError("@Given declaration cannot have a extension receiver")
    }

}