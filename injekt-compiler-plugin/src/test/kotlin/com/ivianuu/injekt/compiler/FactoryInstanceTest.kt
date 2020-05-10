package com.ivianuu.injekt.compiler

import org.junit.Test

class FactoryInstanceTest {

    @Test
    fun testCreateInstance() = codegen(
        """
        @Factory
        fun createBar(): Bar { 
            transient<Foo>()
            transient<Bar>()
            return createInstance()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCreateInstanceAdvanced() = codegen(
        """
        @TestScope 
        class MyClass(foo: Foo, bar: Bar)
        @Factory
        fun createBar(): MyClass {
            scope<TestScope>()
            scoped<Foo>()
            transient<Bar>()
            return createInstance()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testDependencyInInstance() = codegen(
        """
        interface FooOwner {
            val foo: Foo
        }
        
        @Factory
        fun createFoo(fooOwner: FooOwner): Foo {
            dependency(fooOwner)
            return createInstance()
        }
    """
    )

}