package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Provider
import junit.framework.Assert
import org.junit.Test

class ProviderTest {

    @Test
    fun testProviderOfTransient() = codegen(
        """
        interface TestComponent {
            val provider: @Provider () -> Foo
        }
        
        @Factory
        fun create(): TestComponent { 
            transient { Foo() }
            return createImpl()
        }
        
        fun invoke() = create().provider
    """
    ) {
        val provider = invokeSingleFile<@Provider () -> Foo>()
        Assert.assertNotSame(provider(), provider())
    }

    @Test
    fun testProviderOfScoped() = codegen(
        """
        interface TestComponent {
            val provider: @Provider () -> Foo
        }
        
        @Factory
        fun create(): TestComponent { 
            scoped { Foo() }
            return createImpl()
        }
        
        fun invoke() = create().provider
    """
    ) {
        val provider = invokeSingleFile<@Provider () -> Foo>()
        Assert.assertSame(provider(), provider())
    }

    @Test
    fun testQualifiedProvider() = codegen(
        """
        interface TestComponent {
            val provider: @Provider () -> @TestQualifier1 Foo
        }
        
        @Factory
        fun create(): TestComponent { 
            @TestQualifier1 transient { Foo() }
            return createImpl()
        }
        
        fun invoke() = create().provider
    """
    ) {
        assertOk()
    }

}