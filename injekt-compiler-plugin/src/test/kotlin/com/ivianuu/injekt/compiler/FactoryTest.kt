package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertTrue
import org.junit.Test

class FactoryTest {

    @Test
    fun testInlineModuleLambda() = codegen(
        """
        @Factory
        inline fun <T> buildInstance(block: @Module () -> Unit): T { 
            block()
            return createInstance() 
        }
        
        fun invoke() = buildInstance<Foo> { transient<Foo>() }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineModuleLambdaWithArgs() = codegen(
        """
        @Module
        fun calling() {
            inlined { instance(it) }
        }
        
        @Module
        inline fun inlined(block: @Module (String) -> Unit) {
            block("hello world")
        }
        
        @Factory
        fun factory(): String {
            calling()
            return createInstance()
        }
    """
    )

    @Test
    fun testFactoryWithModuleParam() = codegen(
        """
        @Factory
        inline fun factory(block: @Module () -> Unit): Foo {
            block()
            return createInstance()
        }
        
        fun invoke() = factory { 
            transient<Foo>() 
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testFactoryWithModuleParamInClass() = codegen(
        """
        class Lol {
            @Factory
            inline fun factory(block: @Module () -> Unit): Foo {
                block()
                return createInstance()
            }
        }
        
        fun invoke() = Lol().factory { 
            transient<Foo>() 
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

}
