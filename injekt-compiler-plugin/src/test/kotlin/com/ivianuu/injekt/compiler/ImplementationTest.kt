package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import org.junit.Test

class ImplementationTest {

    @Test
    fun testTransient() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { Foo() }
        }
        
        fun invoke() = create().foo
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testScoped() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            scoped { Foo() }
        }
        
        fun invoke() = create().foo
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testInstance() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }
        
        @Factory
        fun create(foo: Foo): TestComponent = createImplementation {
            instance(foo)
        }
        
        fun invoke(foo: Foo) = create(foo).foo
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testInclude() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }
        
        @Module
        fun module(foo: Foo) {
            instance(foo)
        }
        
        @Factory
        fun create(foo: Foo): TestComponent = createImplementation {
            module(foo)
        }
        
        fun invoke(foo: Foo) = create(foo).foo
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testEmpty() = codegen(
        """
        interface TestComponent {
        }
        
        @Factory
        fun create(): TestComponent = createImplementation()
        
        fun invoke() = create()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testQualified() = codegen(
        """
        interface TestComponent {
            val foo1: @TestQualifier1 Foo
            val foo2: @TestQualifier2 Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            @TestQualifier1 scoped { Foo() }
            @TestQualifier2 scoped { Foo() }
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = create()
            return component.foo1 to component.foo2
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

}