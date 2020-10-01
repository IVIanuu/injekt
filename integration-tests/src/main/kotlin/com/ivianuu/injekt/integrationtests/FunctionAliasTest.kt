package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class FunctionAliasTest {

    @Test
    fun testSimpleFunctionAlias() = codegen(
        """
            @Binding
            fun function(string: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    ) {
        assertOk()
    }

    @Test
    fun testAssistedFunctionAlias() = codegen(
        """
            @Binding
            fun function(string: String, @Assisted assistedString: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSuspendFunctionAlias() = codegen(
        """
            @Binding
            suspend fun function(string: String, @Assisted assistedString: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testFunctionAliasWithTypeParameters() = codegen(
        """
            @Binding
            fun <T : S, S> function(t: T): S {
                return error("")
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function<String, String>
            }
        """
    )

    // todo test with composable
}
