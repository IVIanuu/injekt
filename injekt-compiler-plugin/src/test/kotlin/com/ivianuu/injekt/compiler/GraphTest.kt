package com.ivianuu.injekt.compiler

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

}
