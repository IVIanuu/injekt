package com.ivianuu.injekt.compiler

import org.junit.Test

class ModuleTest {

    @Test
    fun testSimple() = codegenTest(
        """ 
        @Module
        fun myModule() {
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegenTest(
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
    fun testWithCaptures() = codegenTest(
        """
        @Module
        fun module(capturedValue: String) {
            factory { capturedValue }
        }
        """
    )

    @Test
    fun testWithNestedCaptures() = codegenTest(
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
    )

    @Test
    fun testModuleDependsOnOtherA() = codegenTest(
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
    )

    @Test
    fun testModuleDependsOnOtherB() = codegenTest(
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
    )

    @Test
    fun testWithParent() = codegenTest(
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
    fun testMultipleParents() = codegenTest(
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
    fun testDuplicatedParentsFails() = codegenTest(
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
    fun testWithScope() = codegenTest(
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
    fun testInvalidScope() = codegenTest(
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
    fun testMultipleScopes() = codegenTest(
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
    fun testDuplicatedScopesFails() = codegenTest(
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
    fun testInclude() = codegenTest(
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
    fun testMultipleIncludes() = codegenTest(
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
    fun testDuplicatedInclude() = codegenTest(
        """
            @Module fun include() {}
            @Module
            fun module() {
                include()
                include()
            }
        """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testCircularModuleDependency() = codegenTest(
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
    fun testCircularParentDependency() = codegenTest(
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
    fun testParentDoesNotExist() = codegenTest(
        """ 
        val parent = Component("parent") {}
        @Module
        fun myModule() {
            parent("wrong key", parent)
        }
        """
    ) {
        assertInternalError("found")
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
