package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class FunBindingTest {

    @Test
    fun testSimpleFunBinding() = codegen(
        """
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
    fun testFunBindingWithExtension() = codegen(
        """
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
    fun testSimpleFunBindingInDifferentPackage() = codegen(
        source(
            """
                @FunBinding
                fun function(string: String) {
                }
            """
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function
                
                @Component
                abstract class TestComponent(@Binding val string: String) {
                    abstract val function: function
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt")
        )
    ) {
        assertOk()
    }

    @Test
    fun testSimpleFunBindingInDifferentPackageComplex() = codegen(
        source(
            """
                @FunBinding
                fun function(string: String) {
                }
            """
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function
                @FunBinding
                fun function2(function: function, string: String) {
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt2")
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function 
                import com.ivianuu.injekt2.function2

                @Component
                abstract class TestComponent(@Binding val string: String) {
                    abstract val function: function
                    abstract val function2: function2
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt")
        )
    ) {
        assertOk()
    }

    @Test
    fun testAssistedFunBinding() = codegen(
        """
            @FunBinding
            fun function(string: String, assistedString: @Assisted String) {
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
            @FunBinding
            fun @Assisted String.function(string: String) {
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
            @FunBinding
            suspend fun @Assisted String.function(string: String) {
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
            @FunBinding
            suspend fun function(string: String, assistedString: @Assisted String) {
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
            @Composable
            @FunBinding
            fun function(string: String, assistedString: @Assisted String) {
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
                    @FunBinding
                    suspend fun function(string: String, assistedString: @Assisted String) {
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
