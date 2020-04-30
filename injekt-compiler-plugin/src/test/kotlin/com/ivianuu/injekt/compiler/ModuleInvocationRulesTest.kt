package com.ivianuu.injekt.compiler

import org.junit.Test

class ModuleInvocationRulesTest {

    @Test
    fun testModuleInvocationInModuleAllowed() = codegen(
        """
            @Module fun a() {}
            @Module fun b() { a() }
        """
    ) {
        assertOk()
    }

    @Test
    fun testModuleInvocationInNonModuleNotAllowed() = codegen(
        """
            @Module fun a() {}
            fun b() { a() }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testModuleInvocationInModuleLambdaIsAllowed() = codegen(
        """
            val lambda: @Module () -> Unit = {
                module()
            }
            @Module fun module() {}
        """
    ) {
        assertOk()
    }

    @Test
    fun testModuleInvocationInNonModuleLambdaIsNotAllowed() = codegen(
        """
            val lambda: () -> Unit = {
                module()
            }
            @Module fun module() {}
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testModuleCannotReturnType() = codegen(
        """
            @Module fun module(): Boolean = true
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testIfNotAllowedAroundModuleInvocation() = codegen(
        """
            @Module fun a() {}
            @Module fun b() {
                if (true) a()
            }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testElseNotAllowedAroundModuleInvocation() = codegen(
        """
            @Module fun a() {}
            @Module fun b() {
                if (true) { } else a()
            }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testWhileNotAllowedAroundModuleInvocation() = codegen(
        """
            @Module fun a() {}
            @Module fun b() {
                while (true) {
                    a()
                }
            }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testForNotAllowedAroundModuleInvocation() = codegen(
        """
            @Module fun a() {}
            @Module fun b() {
                for (i in 0 until 100) {
                    a()
                }
            }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testWhenNotAllowedAroundModuleInvocation() = codegen(
        """
            @Module fun a() {}
            @Module fun b() {
                when {
                    true -> a()
                }
            }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testTryCatchNotAllowedAroundModuleInvocation() = codegen(
        """
            @Module fun a() {}
            @Module fun b() {
                try {
                    a()
                } catch (e: Exception) {
                }
            }
        """
    ) {
        assertCompileError()
    }
}
