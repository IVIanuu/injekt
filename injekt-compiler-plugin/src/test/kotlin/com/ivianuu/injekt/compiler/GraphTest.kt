package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertNotSame
import org.junit.Test

class GraphTest {

    @Test
    fun testMissingBindingFails() = codegen(
        """
        interface TestComponent {
            val dep: Dep
        }

        @Transient
        class Dep(bar: Bar)

        @Factory
        fun create(): TestComponent {
            return createImpl()
        }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDuplicatedBindingFails() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }

        @Factory
        fun create(): TestComponent {
            transient { Foo() }
            transient { Foo() }
            return createImpl()
        }
        """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testCircularDependency() = codegen(
        """
        interface TestComponent {
            val a: A
        }
        
        @Transient class A(b: B)
        @Transient class B(a: A)

        @Factory fun create(): TestComponent = createImpl()
    """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testScopeMismatch() = codegen(
        """
        interface TestComponent {
            val dep: Dep
        }
        
        @TestScope2
        class Dep

        @Factory
        fun create(): TestComponent {
            scope<TestScope>()
            return createImpl()
        }
        """
    ) {
        assertInternalError("scope mismatch")
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
    fun testIgnoresNullability() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }

        @Factory
        fun create(): TestComponent {
            transient<Foo> { Foo() }
            transient<Foo?> { null }
            return createImpl()
        }
    """
    ) {
        assertInternalError()
    }

}
