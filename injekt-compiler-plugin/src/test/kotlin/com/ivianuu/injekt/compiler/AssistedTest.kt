package com.ivianuu.injekt.compiler

import org.junit.Test

class AssistedTest {

    @Test
    fun testAssisted() = codegen(
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

}