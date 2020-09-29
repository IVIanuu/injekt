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
        
        @RootComponentFactory
        typealias Factory = () -> TestComponent
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
        
        @RootComponentFactory
        typealias Factory = () -> TestComponent
    """)

    // todo test with assisted

    // todo test with suspend

    // todo test with composable

}
