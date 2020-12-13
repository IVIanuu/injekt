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
                @Given constructor(foo: Foo = given)
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

    @Test
    fun testGivenPropertyCallWithoutGivenParameter() = codegen(
        """
            fun bar() = given
        """
    ) {
        assertCompileError("given property can only be used as a default value for a parameter")
    }

    @Test
    fun testGivenOrElseCallWithoutGivenParameter() = codegen(
        """
            fun bar() = givenOrElse { Unit }
        """
    ) {
        assertCompileError("givenOrElse can only be used as a default value for a parameter")
    }

}
