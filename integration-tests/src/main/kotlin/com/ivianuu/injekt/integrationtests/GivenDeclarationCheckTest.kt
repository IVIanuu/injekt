package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertMessage
import com.ivianuu.injekt.test.assertNoMessage
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

    @Test
    fun testUsedGivenParameterIsNotMarkedAsUnused() = codegen(
        """
            fun func1(@Given foo: Foo) {
                func2()                
            }

            fun func2(@Given foo: Foo) {
                foo
            } 
        """
    ) {
        assertNoMessage("Parameter 'foo' is never used")
    }

    @Test
    fun testUnusedGivenParameterIsMarkedAsUnused() = codegen(
        """
            fun func1(@Given foo: Foo) {
                func2()                
            }

            fun func2(@Given foo: Foo) {
                foo
            } 
        """
    ) {
        assertMessage("Parameter 'foo' is never used")
    }

}
