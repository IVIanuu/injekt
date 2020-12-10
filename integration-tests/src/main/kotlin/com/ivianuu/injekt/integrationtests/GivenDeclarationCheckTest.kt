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
                @Given constructor(foo: @Given Foo = given)
                @Given constructor(bar: @Given Bar = given)
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

    @Test
    fun testGivenPropertyCallWithoutGivenParameter() = codegen(
        """
            fun bar() = given
        """
    ) {
        assertCompileError("The given property can only be used on a parameter with a @Given type")
    }

    @Test
    fun testGivenPropertyCallWithoutGiven() = codegen(
        """
            fun bar(p1: String = given) = p1
        """
    ) {
        assertCompileError("The given property can only be used on a parameter with a @Given type")
    }

}