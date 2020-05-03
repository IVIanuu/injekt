package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Provider
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
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
        
        val component = create()
        fun invoke() = component.foo
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
    fun testDependency() = codegen(
        """
        interface DependencyComponent {
            val foo: Foo
        }
        
         @Factory
        fun createDep(): DependencyComponent = createImplementation {
            transient { Foo() }
        }
        
        interface TestComponent {
            val bar: Bar
        }

        @Factory
        fun createChild(): TestComponent = createImplementation {
            dependency(createDep())
            transient { Bar(get()) }
        }
        
        fun invoke() = createChild().bar
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testAlias() = codegen(
        """
        interface TestComponent {
            val any: Any
            val foo: Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            scoped { Foo() }
            alias<Foo, Any>()
        }
        
        val component = create()
        fun invoke() = component.foo to component.any
    """
    ) {
        val (foo, any) = (invokeSingleFile() as Pair<Foo, Any>)
        assertSame(foo, any)
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

    // todo @Test
    fun testProviderOfTransient() = codegen(
        """
        interface TestComponent {
            val provider: Provider<Foo>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            transient { Foo() }
        }
        
        fun invoke() = create().provider
    """
    ) {
        val provider = invokeSingleFile<Provider<Foo>>()
        assertNotSame(provider(), provider())
    }

    // todo @Test
    fun testProviderOfScoped() = codegen(
        """
        interface TestComponent {
            val provider: Provider<Foo>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            scoped { Foo() }
        }
        
        fun invoke() = create().provider
    """
    ) {
        val provider = invokeSingleFile<Provider<Foo>>()
        assertSame(provider(), provider())
    }

    // todo @Test
    fun testQualifiedProvider() = codegen(
        """
        interface TestComponent {
            val provider: @TestQualifier1 Provider<Foo>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            @TestQualifier1 transient { Foo() }
        }
        
        fun invoke() = create().provider
    """
    ) {
        assertOk()
    }

}