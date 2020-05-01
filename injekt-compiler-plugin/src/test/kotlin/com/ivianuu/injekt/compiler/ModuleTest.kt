package com.ivianuu.injekt.compiler

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

}
