package com.ivianuu.injekt.compiler

import org.junit.Test

class AssistedTest {

    @Test
    fun testAssistedWithAnnotations() = codegen(
        """
        interface TestComponent {
            val depProvider: @Provider (String) -> Dep
        } 
        
        @Transient
        class Dep(
            @Assisted val assisted: String,
            val foo: Foo
        )
        
        @Factory
        fun create(): TestComponent = createImplementation {
            instance(Foo())
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testAssistedInDsl() = codegen(
        """
        interface TestComponent {
            val depProvider: @Provider (String) -> Dep
        } 
        
        class Dep(val assisted: String, val foo: Foo)
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { Foo() }
            transient {
                Dep(it.component1(), get())
            }
        }
    """
    ) {
        assertOk()
    }

}