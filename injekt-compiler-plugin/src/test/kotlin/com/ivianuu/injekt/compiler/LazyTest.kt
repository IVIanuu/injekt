package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Provider
import junit.framework.Assert
import org.junit.Test

class LazyTest {

    @Test
    fun testLazyOfTransient() = codegen(
        """
        interface TestComponent {
            val lazy: @Lazy () -> Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            transient { Foo() }
        }
        
        fun invoke() = create().lazy
    """
    ) {
        val lazy = invokeSingleFile<@Lazy () -> Foo>()
        Assert.assertSame(lazy(), lazy())
    }

    @Test
    fun testLazyOfScoped() = codegen(
        """
        interface TestComponent {
            val lazy: @Lazy () -> Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            scoped { Foo() }
        }
        
        fun invoke() = create().lazy
    """
    ) {
        val lazy = invokeSingleFile<@Lazy () -> Foo>()
        Assert.assertSame(lazy(), lazy())
    }

    @Test
    fun testQualifiedLazy() = codegen(
        """
        interface TestComponent {
            val lazy: @Lazy () -> @TestQualifier1 Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            @TestQualifier1 transient { Foo() }
        }
        
        fun invoke() = create().lazy
    """
    ) {
        assertOk()
    }

    @Test
    fun testProviderOfLazy() = codegen(
        """
        interface TestComponent {
            val providerOfLazy: @Provider () -> @Lazy () -> Foo
        }
        
        @Factory
        fun create(): TestComponent = createImplementation { 
            transient { Foo() }
        }
        
        val component = create()
        fun invoke() = component.providerOfLazy
    """
    ) {
        val lazyA = invokeSingleFile<@Provider () -> @Lazy () -> Foo>()()
        val lazyB = invokeSingleFile<@Provider () -> @Lazy () -> Foo>()()
        Assert.assertNotSame(lazyA, lazyB)
        Assert.assertSame(lazyA(), lazyA())
        Assert.assertSame(lazyB(), lazyB())
        Assert.assertNotSame(lazyA(), lazyB())
    }

}