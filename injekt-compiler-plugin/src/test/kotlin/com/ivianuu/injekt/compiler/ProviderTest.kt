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
        fun create(): TestComponent = createImplementation { 
            transient { Foo() }
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
        fun create(): TestComponent = createImplementation { 
            scoped { Foo() }
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
        fun create(): TestComponent = createImplementation { 
            @TestQualifier1 transient { Foo() }
        }
        
        fun invoke() = create().provider
    """
    ) {
        assertOk()
    }

}