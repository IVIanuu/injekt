package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertEquals
import org.junit.Test

class ModuleTest {

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
        fun module() {
            instance("")
        }
    """
    ) {
        assertOk()
        val descriptorClass = classLoader.loadClass("module_Impl").declaredClasses
            .single { it.name == "module_Impl\$Descriptor" }
        println(descriptorClass)
        val methods = descriptorClass.declaredMethods
        methods[0].let {
            assertEquals(0, it.parameterCount)
        }

    }

    @Test
    fun testQualifiedExpression() = codegen(
        """
        @Module
        fun module() {
            @TestQualifier1
            set<String>()
        }
    """
    ) {
    }

    @Test
    fun testAlias() = codegen(
        """
        @Module
        fun module() {
            alias<@TestQualifier1 String, @TestQualifier2 Any>()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testBinding() = codegen(
        """
        @Module
        fun module() {
            @TestQualifier1
            transient { (p0: String, p1: String) ->
                get<Int>().toString()
            }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testMap() = codegen(
        """
        @Module
        fun module() {
            map<String, String> {
                put<@TestQualifier1 String>("hello")
            }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testSet() = codegen(
        """
        @Module
        fun module() {
            set<String> {
                add<@TestQualifier1 String>()
            }
        }
    """
    ) {

        assertOk()
    }

}
