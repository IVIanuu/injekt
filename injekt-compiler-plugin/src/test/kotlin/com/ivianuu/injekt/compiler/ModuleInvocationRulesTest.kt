package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegenTest
import org.junit.Test

class ModuleInvocationRulesTest {

    @Test
    fun testModuleInvocationInModuleAllowed() = codegenTest(
        """
            @Module fun a() {}
            @Module fun b() { a() }
        """
    ) {
        assertOk()
    }

    @Test
    fun testModuleInvocationInNonModuleNotAllowed() = codegenTest(
        """
            @Module fun a() {}
            fun b() { a() }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testModuleInvocationInModuleLambdaIsAllowed() = codegenTest(
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
    fun testModuleInvocationInNonModuleLambdaIsNotAllowed() = codegenTest(
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
    fun testModuleCannotReturnType() = codegenTest(
        """
            @Module fun module(): Boolean = true
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testIfNotAllowedAroundModuleInvocation() = codegenTest(
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
    fun testElseNotAllowedAroundModuleInvocation() = codegenTest(
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
    fun testWhileNotAllowedAroundModuleInvocation() = codegenTest(
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
    fun testForNotAllowedAroundModuleInvocation() = codegenTest(
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
    fun testWhenNotAllowedAroundModuleInvocation() = codegenTest(
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
    fun testTryCatchNotAllowedAroundModuleInvocation() = codegenTest(
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
