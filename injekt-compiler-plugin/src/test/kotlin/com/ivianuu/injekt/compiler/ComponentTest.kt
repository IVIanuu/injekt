package com.ivianuu.injekt.compiler

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
        fun ComponentDsl.module() {
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
        fun ComponentDsl.a() {
            factory { "" }
            b()
        }
        
        @Module
        fun ComponentDsl.b() { 
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
        fun ComponentDsl.module(capturedValue: String) {
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
