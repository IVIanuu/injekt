package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertEquals
import org.junit.Test

class ModuleTest {

    @Test
    fun testSimple() = codegen(
        """ 
        @Module
        fun myModule() {
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegen(
        """ 
        @Module
        fun myModule() {
            factory { "" }
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithCaptures() = codegen(
        """
        @Module
        fun module(capturedValue: String) {
            factory { capturedValue }
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithNestedCaptures() = codegen(
        """
        @Module
        fun a(capturedValue: String) {
            b(capturedValue)
        }
        
        @Module
        fun b(capturedValue: String) {
            factory { capturedValue }
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testModuleDependsOnOtherA() = codegen(
        source(
            """
            @Module 
            fun a(capturedValue: String) { 
                b(capturedValue) 
            }
        """
        ),
        source(
            """
                @Module 
                fun b(capturedValue: String) { 
                    factory { capturedValue } 
                }
        """
        )
    ) {
        assertOk()
    }

    @Test
    fun testModuleDependsOnOtherB() = codegen(
        source(
            """
                @Module 
                fun b(capturedValue: String) { 
                    factory { capturedValue } 
                }
        """
        ),
        source(
            """
            @Module 
            fun a(capturedValue: String) { 
                b(capturedValue) 
            }
        """
        )
    ) {
        assertOk()
    }

    @Test
    fun testWithParent() = codegen(
        """
            val parent = Component("parent") {
            }
            
            @Module
            fun module() {
                parent("parent", parent)
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testMultipleParents() = codegen(
        """
            val parentA = Component("a") {
            }
            val parentB = Component("b") {
            }
            
            @Module
            fun module() {
                parent("a", parentA)
                parent("b", parentB)
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testDuplicatedParentsFails() = codegen(
        """
            val parent = Component("parent") { }
            @Module
            fun module() {
                parent("parent", parent)
                parent("parent", parent)
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithScope() = codegen(
        """
            @Module
            fun module() {
                scope<TestScope>()
            }
            """
    ) {
        assertOk()
    }

    @Test
    fun testInvalidScope() = codegen(
        """
            @Module
            fun module() {
                scope<String>()
            }
            """
    ) {
        assertCompileError("not")
    }

    @Test
    fun testMultipleScopes() = codegen(
        """
            @Module
            fun module() {
                scope<TestScope>()
                scope<TestScope2>()
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testDuplicatedScopesFails() = codegen(
        """
            @Module
            fun module() {
                scope<TestScope>()
                scope<TestScope>()
            }
        """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testInclude() = codegen(
        """
            @Module
            fun module() {
                scope<TestScope>()
            }
            """
    ) {
        assertOk()
    }

    @Test
    fun testMultipleIncludes() = codegen(
        """
            @Module fun includeA() {}
            @Module fun includeB() {}
            @Module
            fun module() {
                includeA()
                includeB()
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testCircularModuleDependency() = codegen(
        """
            @Module
            fun a() {
                b()
            }
            
            @Module
            fun b() {
                a()
            }
        """
    ) {
        assertInternalError()
    }

    @Test
    fun testCircularParentDependency() = codegen(
        """
            val parent = Component("c") {
                module()
            }
            
            @Module
            fun module() {
                parent("c", parent)
            }
        """
    ) {
        assertInternalError()
    }

    @Test
    fun testParentDoesNotExist() = codegen(
        """ 
        val parent = Component("parent")
        @Module
        fun myModule() {
            parent("wrong key", parent)
        }
        """
    ) {
        assertInternalError("found")
    }

    @Test
    fun testImplicitModule() = codegen(
        """ 
            @TestScope 
            @Module 
            fun module() {
                factory { "test" }
            }
            
            val Component = Component("c") {
                scope<TestScope>()
            }
            
            fun invoke() = Component.get<String>()
        """
    ) {
        assertEquals("test", invokeSingleFile())
    }

    @Test
    fun testImplicitModuleWithValueParameters() = codegen(
        """ 
            @TestScope 
            @Module 
            fun module(p1: String) {}
        """
    ) {
        assertCompileError("value parameter")
    }

    @Test
    fun testImplicitModuleWithTypeParameters() = codegen(
        """ 
            @TestScope 
            @Module 
            fun <T> module() {}
        """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testLocalDeclarationCapturingModule() = codegen(
        """ 
            @Module 
            fun module() {
               val local = "local"
               factory { local }
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testLocalDelegateCapturingModule() = codegen(
        """ 
            @Module 
            fun module() {
               val local by lazy { "local" }
               factory { local }
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testLocalFunctionInModule() = codegen(
        """ 
            @Module 
            fun module() {
               fun localFun() = "local"
               val local = localFun()
               factory { local }
            }
        """
    ) {
        assertOk()
    }

    /**@Test
    fun testMetadata() = codegenTest(
    """
    val parent = Component("c") {
    }

    @Module
    fun module() {

    }
    """
    )*/
}
