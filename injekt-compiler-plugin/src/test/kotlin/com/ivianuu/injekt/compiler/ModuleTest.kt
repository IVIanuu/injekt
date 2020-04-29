package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertEquals
import org.junit.Test

class ModuleTest {

    @Test
    fun testModule() = codegen(
        """
        @Module
        fun module() {
            factory { "hello world" }
        }
        
        fun invoke(): String {
            return Component {
                module()
            }.get<String>()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun testCapturingModule() = codegen(
        """
        @Module
        fun module(capture: String) {
            factory { capture }
        }
        
        fun invoke(): String {
            return Component {
                module("hello world")
            }.get<String>()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun testBindFragment() = codegen(
        """
        @Module 
        fun module() { 
            factory { Bar(get()) } 
            }

        @Module
        inline fun <reified T : Any> bindFragment(
            qualifier: kotlin.reflect.KClass<*>? = null
        ): Unit = injektIntrinsic()
    """
    ) {
        assertOk()
    }

    @Test
    fun testImplicitModule() = codegen(
        """
        @TestScope1
        @Module
        fun module() {
            factory { "hello world" }
        }
        
        fun invoke(): String {
            Injekt.initializeEndpoint()
            return Component<TestScope1>().get<String>()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

}