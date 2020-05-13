package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.codegen
import org.junit.Test

class ClassOfTest {

    @Test
    fun testClassOfInNonModule() = codegen(
        """
        fun <T> module() { classOf<T>() }
    """
    ) {
        assertCompileError("module")
    }

    @Test
    fun testClassOfInNonInlineModule() = codegen(
        """
        @Module fun <T> module() { classOf<T>() }
    """
    ) {
        assertCompileError("inline")
    }

    @Test
    fun testClassOfWithReified() = codegen(
        """
        @Module inline fun <reified T> module() { classOf<T>() }
    """
    ) {
        assertCompileError("reified")
    }

    @Test
    fun testClassOfWithConcreteType() = codegen(
        """
        @Module inline fun module() { classOf<String>() }
    """
    ) {
        assertCompileError("generic")
    }

}