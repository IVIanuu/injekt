package com.ivianuu.injekt.compiler

import org.junit.Test

class ModuleTest {

    @Test
    fun testSimple() = codegenTest(
        """ 
        @Module
        fun ComponentDsl.myModule() {
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegenTest(
        """ 
        @Module
        fun ComponentDsl.myModule() {
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
        fun ComponentDsl.module(capturedValue: String) {
            factory { capturedValue }
        }
        """
    )

    @Test
    fun testWithNestedCaptures() = codegenTest(
        """
        @Module
        fun ComponentDsl.a(capturedValue: String) {
            b(capturedValue)
        }
        
        @Module
        fun ComponentDsl.b(capturedValue: String) {
            factory { capturedValue }
        }
        """
    )

    @Test
    fun testModuleDependsOnOtherA() = codegenTest(
        source(
            """
            @Module 
            fun ComponentDsl.a(capturedValue: String) { 
                b(capturedValue) 
            }
        """
        ),
        source(
            """
                @Module 
                fun ComponentDsl.b(capturedValue: String) { 
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
                fun ComponentDsl.b(capturedValue: String) { 
                    factory { capturedValue } 
                }
        """
        ),
        source(
            """
            @Module 
            fun ComponentDsl.a(capturedValue: String) { 
                b(capturedValue) 
            }
        """
        )
    )


}