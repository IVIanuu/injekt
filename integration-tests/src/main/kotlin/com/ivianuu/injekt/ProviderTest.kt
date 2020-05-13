package com.ivianuu.injekt

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
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
        assertNotSame(provider(), provider())
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
        assertSame(provider(), provider())
    }

    @Test
    fun testQualifiedProvider() = codegen(
        """
            @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
            @Qualifier
            annotation class TestQualifier1
        @Factory
        fun invoke(): @Provider () -> @TestQualifier1 Foo { 
            @TestQualifier1 transient { Foo() }
            return createInstance()
        }
         """
    ) {
        assertOk()
    }

}