package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.assertOk
import com.ivianuu.injekt.codegen
import org.junit.Test

class ModuleDslTest {

    @Test
    fun testSupportedChildFactory() = codegen(
        """
        @ChildFactory
        fun factory(): TestComponent {
            return createImpl()
        }
        
        @Module
        fun module() {
            childFactory(::factory)
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testUnsupportedChildFactory() = codegen(
        """
        fun factory() {
        }
        
        @Module
        fun module() {
            childFactory(::factory)
        }
    """
    ) {
        assertCompileError("@ChildFactory")
    }

    @Test
    fun testModuleInvocationInModuleAllowed() =
        codegen(
            """
            @Module fun a() {}
            @Module fun b() { a() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testModuleInvocationInNonModuleNotAllowed() =
        codegen(
            """
            @Module fun a() {}
            fun b() { a() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testModuleInvocationInFactoryAllowed() =
        codegen(
            """
            @Module fun module() {}
            @Factory fun factory(): TestComponent { 
                module()
                return createImpl() 
            }
        """
        ) {
            assertOk()
        }

    @Test
    fun testModuleInvocationInChildFactoryAllowed() =
        codegen(
            """
            @Module fun module() {}
            @ChildFactory fun factory(): TestComponent { 
                module()
                return createImpl() 
            }
        """
        ) {
            assertOk()
        }

    @Test
    fun testModuleInvocationInNonModuleLambdaIsNotAllowed() =
        codegen(
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
    fun testIfNotAllowedAroundModuleInvocation() =
        codegen(
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
    fun testElseNotAllowedAroundModuleInvocation() =
        codegen(
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
    fun testWhileNotAllowedAroundModuleInvocation() =
        codegen(
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
    fun testForNotAllowedAroundModuleInvocation() =
        codegen(
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
    fun testWhenNotAllowedAroundModuleInvocation() =
        codegen(
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
    fun testTryCatchNotAllowedAroundModuleInvocation() =
        codegen(
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

    @Test
    fun testSupportedScope() = codegen(
        """
        @Module
        fun test() { 
            scope<TestScope>()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testUnsupportedScope() = codegen(
        """
        @Module
        fun test() {
            scope<Any>()
        }
    """
    ) {
        assertCompileError("@Scope")
    }

    @Test
    fun testModuleCannotBeSuspend() = codegen(
        """
        @Module
        suspend fun module(): TestComponent = createImpl()
        """
    ) {
        assertCompileError("suspend")
    }

    @Test
    fun testBindingWithTypeParameterInNonInlineModule() =
        codegen(
            """ 
        @Module
        fun <T> module() {
            transient<T>()
        }
    """
        ) {
            assertCompileError("inline")
        }

    @Test
    fun testInlineModuleWithDefinition() = codegen(
        """ 
        @Module
        inline fun <T> module(definition: ProviderDefinition<T>) {
        }
    """
    )

    @Test
    fun testNonInlineModuleWithDefinition() = codegen(
        """ 
        @Module
        fun <T> module(definition: ProviderDefinition<T>) {
        }
    """
    ) {
        assertCompileError("inline")
    }

}
