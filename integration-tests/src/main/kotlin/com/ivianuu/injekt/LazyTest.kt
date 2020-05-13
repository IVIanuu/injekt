package com.ivianuu.injekt

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import org.junit.Test

class LazyTest {

    @Test
    fun testLazyOfTransient() = codegen(
        """
        @Factory
        fun invoke(): @Lazy () -> Foo { 
            transient { Foo() }
            return createInstance()
        }
         """
    ) {
        val lazy = invokeSingleFile<@Lazy () -> Foo>()
        assertSame(lazy(), lazy())
    }

    @Test
    fun testLazyOfScoped() = codegen(
        """
        @Factory
        fun invoke(): @Lazy () -> Foo { 
            scoped { Foo() }
            return createInstance()
        }
         """
    ) {
        val lazy = invokeSingleFile<@Lazy () -> Foo>()
        assertSame(lazy(), lazy())
    }

    @Test
    fun testQualifiedLazy() = codegen(
        """
            @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
            @Qualifier
            annotation class TestQualifier1
        @Factory
        fun invoke(): @Lazy () -> @TestQualifier1 Foo { 
            @TestQualifier1 transient { Foo() }
            return createInstance()
        }
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
        fun create(): TestComponent { 
            transient { Foo() }
            return createImpl()
        }
        
        val component = create()
        fun invoke() = component.providerOfLazy
    """
    ) {
        val lazyA =
            invokeSingleFile<@Provider () -> @Lazy () -> Foo>()()
        val lazyB =
            invokeSingleFile<@Provider () -> @Lazy () -> Foo>()()
        assertNotSame(lazyA, lazyB)
        assertSame(lazyA(), lazyA())
        assertSame(lazyB(), lazyB())
        assertNotSame(lazyA(), lazyB())
    }

}