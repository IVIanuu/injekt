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

}