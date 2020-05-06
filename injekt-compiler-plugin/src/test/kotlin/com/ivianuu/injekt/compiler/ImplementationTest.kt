package com.ivianuu.injekt.compiler

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
        fun create(): TestComponent {
            transient { Foo() }
            return createImpl()
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
        fun create(): TestComponent {
            scoped { Foo() }
            return createImpl()
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
        fun create(foo: Foo): TestComponent {
            instance(foo)
            return createImpl()
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
        fun create(foo: Foo): TestComponent {
            module(foo)
            return createImpl()
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
        fun createDep(): DependencyComponent {
            transient { Foo() }
            return createImpl()
        }
        
        interface TestComponent {
            val bar: Bar
        }

        @Factory
        fun createChild(): TestComponent {
            dependency(createDep())
            transient { Bar(get()) }
            return createImpl()
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
        fun create(): TestComponent {
            scoped { Foo() }
            alias<Foo, Any>()
            return createImpl()
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
        fun create(): TestComponent = createImpl()
        
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
        fun create(): TestComponent {
            @TestQualifier1 scoped { Foo() }
            @TestQualifier2 scoped { Foo() }
            return createImpl()
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

    @Test
    fun testQualifiedWithValues() = codegen(
        """
            @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
            @Qualifier
            annotation class QualifierWithValue(val value: String)
            
        interface TestComponent {
            val foo1: @QualifierWithValue("A") Foo
            val foo2: @QualifierWithValue("B") Foo
        }
        
        @Factory
        fun create(): TestComponent {
            @QualifierWithValue("A") scoped { Foo() }
            @QualifierWithValue("B") scoped { Foo() }
            return createImpl()
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

    @Test
    fun testFactoryImplementationBinding() = codegen(
        """
        interface TestComponent {
            val dep: Dep
        }
        
        @Transient class Dep(val testComponent: TestComponent)
        
        @Factory
        fun create(): TestComponent = createImpl()
        
        fun invoke(): Pair<TestComponent, TestComponent> = create().let {
            it to it.dep.testComponent
        }
    """
    ) {
        val (component, dep) = invokeSingleFile<Pair<*, *>>()
        assertSame(component, dep)
    }

}