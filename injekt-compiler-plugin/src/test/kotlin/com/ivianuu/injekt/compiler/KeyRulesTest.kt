package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class KeyRulesTest {

    @Test
    fun testComponentConstantKey() = codegen(
        """
            const val key = "key"
            val TestComponent = Component(key) { }
                """
    ) {
        assertOk()
    }

    @Test
    fun testComponentNonConstantKeyFails() = codegen(
        """
            fun key() = ""
            val TestComponent = Component(key()) { }
                """
    ) {
        assertCompileError("constant")
    }

    @Test
    fun testModuleConstantParentKey() = codegen(
        """ 
        val parent = Component("parent")
        @Module
        fun myModule() {
            parent("parent", parent)
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testModuleNotConstantParentKey() = codegen(
        """ 
        val parent = Component("parent")
        fun key() = "parent"
        @Module
        fun myModule() {
            parent(key(), parent)
        }
        """
    ) {
        assertCompileError("constant")
    }

}