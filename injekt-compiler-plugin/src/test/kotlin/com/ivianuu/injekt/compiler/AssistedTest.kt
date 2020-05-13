package com.ivianuu.injekt.compiler

import org.junit.Test

class AssistedTest {

    @Test
    fun testAssistedWithAnnotations() = codegen(
        """
        @Transient
        class Dep(
            @Assisted val assisted: String,
            val foo: Foo
        )
        
        @Factory
        fun create(): @Provider (String) -> Dep {
            instance(Foo())
            return createInstance()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testAssistedInDsl() = codegen(
        """
        class Dep(val assisted: String, val foo: Foo)
        
        @Factory
        fun create(): @Provider (String) -> Dep {
            transient { Foo() }
            transient { (assisted: String) -> Dep(assisted, get()) }
            return createInstance()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testAssistedInDsl2() = codegen(
        """
        class Dep(val assisted: String, val foo: Foo)
        
        @Factory
        fun create(): @Provider (String) -> Dep {
            transient { Foo() }
            transient { Dep(it.component1(), get()) }
            return createInstance()
        }
    """
    ) {
        assertOk()
    }

}