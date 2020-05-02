package com.ivianuu.injekt.compiler.dsl

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class ModuleDslTest {

    @Test
    fun testSupportedChildFactory() = codegen(
        """
        @ChildFactory
        fun factory(): TestComponent {
            return createImplementation()
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
    fun testModuleDescriptor() = codegen(
        """ 
        @Module
        fun <T> other(instance: T) {
        }
        
        interface Dependency
        
        @ChildFactory
        fun myChildFactory(): TestComponent = createImplementation()
        
        @Module
        fun module(dependency: Dependency) {
            other("")
            
            instance("")
            
            dependency(dependency)
            
            childFactory(::myChildFactory)
            
            @TestQualifier1
            transient { (p0: String, p1: String) ->
                get<Int>().toString()
            }
            
            alias<@TestQualifier1 String, @TestQualifier2 Any>()
            
            @TestQualifier1
            map<kotlin.reflect.KClass<*>, String> {
                put<@TestQualifier1 String>(String::class)
            }
            
            @TestQualifier1
            set<String> {
                add<@TestQualifier1 String>()
            }
        }
    """
    ) {
        assertOk()
        val descriptorClass = classLoader.loadClass("module_Impl").declaredClasses
            .single { it.name == "module_Impl\$Descriptor" }
        println(descriptorClass)
        /*val methods = descriptorClass.declaredMethods
        methods[0].let {
            assertEquals(0, it.parameterCount)
        }*/

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
    fun testModuleInvocationInModuleLambdaIsAllowed() =
        codegen(
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
}
