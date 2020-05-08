package com.ivianuu.injekt.compiler.dsl

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class MapDslTest {

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
            map<KClass<*>, Any> {
                put<String>(String::class)
            }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testClassOfMapKey() = codegen(
        """
        @Module
        fun test() { 
            map<KClass<*>, Any> {
                put<String>(classOf<String>())
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
            map<KClass<*>, Any> {
                put<String>(key())
            }
        }
    """
    ) {
        assertCompileError("constant")
    }

}
