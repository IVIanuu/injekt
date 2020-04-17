package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegenTest
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimpleComponent() = codegenTest(
        """
        val MyComponent = Component("c") {

        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "" }
            factory { 0 }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithModule() = codegenTest(
        """
        @Module
        fun ComponentDsl.module() {
            factory { "" }
            factory { 0 }
        }
        
        val MyComponent = Component("c") {
            module()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithNestedModule() = codegenTest(
        """
        @Module
        fun ComponentDsl.a() {
            factory { "" }
            b()
        }
        
        @Module
        fun ComponentDsl.b() { 
            factory { 0 }
        }
        
        val MyComponent = Component("c") {
            a()
        }
    """
    ) {
        assertOk()
    }

}
