package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Component
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegenTest(
        """
        val MyComponent = Component("c") {

        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "" }
            factory { 0 }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithModule() = codegenTest(
        """
        @Module
        fun module() {
            factory { "" }
            factory { 0 }
        }
        
        val MyComponent = Component("c") {
            module()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithNestedModule() = codegenTest(
        """
        @Module
        fun a() {
            factory { "" }
            b()
        }
        
        @Module
        fun b() { 
            factory { 0 }
        }
        
        val MyComponent = Component("c") {
            a()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testDuplicatedBinding() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "a" }
            factory { "b" }
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testMissingBinding() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory<String> { get<Int>(); "" }
        }
    """
    ) {
        assertInternalError("missing")
    }

    @Test
    fun testWithCaptures() = codegenTest(
        """
        fun MyComponent(capturedValue: String) = Component("c") {
            factory { capturedValue }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithCaptureModule() = codegenTest(
        """
        @Module
        fun module(capturedValue: String) {
            factory { capturedValue }
        }
            
        fun MyComponent(capturedValue: String) = Component("c") { 
            module(capturedValue)
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testOrder() = codegenTest(
        """
            class Foo
            class Bar(foo: Foo)
            val MyComponent = Component("c") {
                factory { Foo() }
                factory { Bar(get()) }
                }
                fun invoke() = MyComponent

                """
    ) {
        expectNoErrorsWhileInvokingSingleFile()
    }

    @Test
    fun testReverseOrder() = codegenTest(
        """
            class Foo
            class Bar(foo: Foo)
            val MyComponent = Component("c") { 
                factory { Bar(get()) }
                factory { Foo() } 
                }
                
                fun invoke() = MyComponent
                """
    ) {
        expectNoErrorsWhileInvokingSingleFile()
    }

    @Test
    fun testFactory() = codegenTest(
        """
            val MyComponent = Component("c") { 
                factory { Foo() } 
                }
                
                fun invoke() = MyComponent
                """
    ) {
        val component = invokeSingleFile() as Component
        assertNotSame(
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo"),
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo")
        )
    }

    @Test
    fun testSingle() = codegenTest(
        """
            val MyComponent = Component("c") { 
                single { Foo() } 
                }
                
                fun invoke() = MyComponent
                """
    ) {
        val component = invokeSingleFile() as Component
        assertSame(
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo"),
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo")
        )
    }

    /*@Test
    fun test() = codegenTest(
    """
            val MyComponent = Component("c") { 
                factory { Foo() } 
                factory { Bar(get()) }
            }
            
            fun invoke() = MyComponent.get<Bar>()
    """
    ) {
        assertOk()
        invokeSingleFile() is Bar
    }*/

}
