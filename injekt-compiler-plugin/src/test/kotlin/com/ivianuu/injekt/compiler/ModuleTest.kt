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
