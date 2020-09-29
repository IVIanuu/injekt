package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class FunctionAliasTest {

    @Test
    fun testSimpleFunctionAlias() = codegen("""
        @Given
        fun function(string: String) {
        }
        
        interface TestComponent {
            val function: function
        }
        
        @RootFactory
        typealias Factory = (String) -> TestComponent
    """) {
        assertOk()
    }

    @Test
    fun testAssistedFunctionAlias() = codegen("""
        @Given
        fun function(string: String, @Assisted assistedString: String) {
        }
        
        interface TestComponent {
            val function: function
        }
        
        @RootFactory
        typealias Factory = (String) -> TestComponent
    """)

    @Test
    fun testSuspendFunctionAlias() = codegen("""
        @Given
        suspend fun function(string: String, @Assisted assistedString: String) {
        }
        
        interface TestComponent {
            val function: function
        }
        
        @RootFactory
        typealias Factory = (String) -> TestComponent
    """)

    @Test
    fun testFunctionAliasWithTypeParameters() = codegen("""
        @Given
        fun <T : S, S> function(t: T): S {
            return error("")
        }
        
        interface TestComponent {
            val function: function<String, String>
        }
        
        @RootFactory
        typealias Factory = (String) -> TestComponent
    """)

    // todo test with composable

}
