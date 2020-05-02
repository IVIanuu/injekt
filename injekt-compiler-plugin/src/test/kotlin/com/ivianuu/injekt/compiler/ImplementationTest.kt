package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ImplementationTest {

    @Test
    fun testSimple() = codegen(
        """
        interface TestComponent {
            val bar: Bar
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { Foo() }
            transient { Bar(get()) }
        }
        
        fun invoke() = create().bar
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

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

}