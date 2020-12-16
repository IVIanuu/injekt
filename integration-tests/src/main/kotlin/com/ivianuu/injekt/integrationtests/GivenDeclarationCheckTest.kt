package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class GivenDeclarationCheckTest {

    @Test
    fun testClassWithGivenAnnotationAndGivenConstructor() = codegen(
        """
            @Given class Dep @Given constructor()
        """
    ) {
        assertCompileError("Class cannot be given and have a given constructor")
    }

    @Test
    fun testClassWithMultipleGivenConstructors() = codegen(
        """
            class Dep {
                @Given constructor(@Given foo: Foo)
                @Given constructor(bar: Bar = given)
            }
        """
    ) {
        assertCompileError("Class cannot have multiple given constructors")
    }

    @Test
    fun testNonGivenValueParameterOnGivenDeclaration() = codegen(
        """
            @Given fun bar(foo: Foo) = Bar(foo)
        """
    ) {
        assertCompileError("Non @Given value parameter")
    }

    @Test
    fun testGivenWithNonGivenExtensionReceiver() = codegen(
        """
            @Given fun Foo.bar() = Bar(this)
        """
    ) {
        assertCompileError("@Given declaration extension receiver must be annotated with @Given")
    }
}
