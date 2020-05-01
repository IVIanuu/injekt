package com.ivianuu.injekt.compiler

import org.junit.Test

class MapTest {

    @Test
    fun testSupportedMapKeyType() = codegen(
        """
        @Module
        fun test() { 
            map<String, Any>()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testUnsupportedMapKeyType() = codegen(
        """
        @Module
        fun test() { 
            map<Any, Any>()
        }
    """
    ) {
        assertCompileError("map key")
    }

    @Test
    fun testConstantMapKey() = codegen(
        """
        @Module
        fun test() { 
            map<kotlin.reflect.KClass<*>, Any> {
                put<String>(String::class)
            }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testDynamicMapKey() = codegen(
        """
        fun key() = String::class
        @Module
        fun test() { 
            map<kotlin.reflect.KClass<*>, Any> {
                put<String>(key())
            }
        }
    """
    ) {
        assertCompileError("constant")
    }

}
