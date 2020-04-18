package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegenTest
import org.junit.Test

class KeyRulesTest {

    @Test
    fun testComponentConstantKey() = codegenTest(
        """
            const val key = "key"
            val MyComponent = Component(key) { }
                """
    ) {
        assertOk()
    }

    @Test
    fun testComponentNonConstantKeyFails() = codegenTest(
        """
            fun key() = ""
            val MyComponent = Component(key()) { }
                """
    ) {
        assertCompileError("constant")
    }

    @Test
    fun testModuleConstantParentKey() = codegenTest(
        """ 
        val parent = Component("parent") {}
        @Module
        fun myModule() {
            parent("parent", parent)
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testModuleNotConstantParentKey() = codegenTest(
        """ 
        val parent = Component("parent") {}
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