package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class FunBindingTest {

    @Test
    fun testSimpleFunBinding() = codegen(
        """
            typealias function = () -> Unit
            @FunBinding
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
    fun testFunBindingWithAssistedExtension() = codegen(
        """
            typealias function = String.() -> Unit
            @FunBinding
            fun String.function() {
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
    fun testFunBindingWithExtension() = codegen(
        """
            typealias function = () -> Unit
            @FunBinding
            fun String.function() {
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
    fun testAssistedFunBinding() = codegen(
        """
            typealias function = (String) -> Unit
            @FunBinding
            fun function(string: String, assistedString: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testAssistedExtensionFunBinding() = codegen(
        """
            typealias function = String.() -> Unit
            @FunBinding
            fun String.function(string: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testAssistedExtensionSuspendFunBinding() = codegen(
        """
            typealias function = suspend String.() -> Unit
            @FunBinding
            suspend fun String.function(string: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSuspendFunBinding() = codegen(
        """
            typealias function = suspend (String) -> Unit
            @FunBinding
            suspend fun function(string: String, assistedString: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testComposableFunBinding() = codegen(
        """
            typealias function = @Composable (String) -> Unit
            @Composable
            @FunBinding
            fun function(string: String, assistedString: String) {
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSuspendFunBindingMulti() = multiCodegen(
        listOf(
            source(
                """
                    typealias function = suspend (String) -> Unit
                    @FunBinding
                    suspend fun function(string: String, assistedString: String) {
                    }
                    
                    @FunBinding
                    fun usage(function: function) {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class TestComponent(@Binding val string: String) {
                        abstract val function: function
                        abstract val usage: usage
                    } 
                """
            )
        )
    )

    @Test
    fun testFunBindingWithTypeParameters() = codegen(
        """
            typealias function<T, S> = (T) -> S 
            @FunBinding
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
