package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class GivenFunTest {

    @Test
    fun testSimpleGivenFun() = codegen(
        """
            @GivenFun fun function(@Given string: String) {
            }
            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testGivenFunWithExtension() = codegen(
        """
            @GivenFun fun String.function() {
            }

            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testSimpleGivenFunInDifferentPackage() = codegen(
        source(
            """
                @GivenFun fun function(string: String) {
                }
            """
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function
                
                @Component abstract class TestComponent(@Given val string: String) {
                    abstract val function: function
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt")
        )
    )

    @Test
    fun testSimpleGivenFunInDifferentPackageComplex() = codegen(
        source(
            """
                @GivenFun fun function(string: String) {
                }
            """
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function
                @GivenFun fun function2(function: function, string: String) {
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt2")
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function 
                import com.ivianuu.injekt2.function2
                
                fun invoke(@Given string: String) = given<function>() to given<function2>()
            """,
            packageFqName = FqName("com.ivianuu.injekt")
        )
    )

    @Test
    fun testAssistedGivenFun() = codegen(
        """
            @GivenFun fun function(string: String, @FunApi assisted: Int) {
            }
            
            @Component abstract class TestComponent(@Given val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testAssistedGivenFunMulti() = multiCodegen(
        listOf(
            source(
                """
                    @GivenFun fun function(string: String, @FunApi assisted: Int) {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component abstract class TestComponent(@Given val string: String) {
                        abstract val function: function
                    }
                """
            )
        )
    )

    @Test
    fun testAssistedExtensionGivenFun() = codegen(
        """
            @GivenFun fun @receiver:FunApi String.function(string: String) {
            }
            
            @Component abstract class TestComponent(@Given val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testAssistedExtensionSuspendGivenFun() = codegen(
        """
            @GivenFun
            suspend fun @receiver:FunApi String.function(string: String) {
            }
           
            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testSuspendGivenFun() = codegen(
        """
            @GivenFun
            suspend fun function(string: String, @FunApi assistedString: String) {
            }
            
            @Component abstract class TestComponent(@Given val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testComposableGivenFun() = codegen(
        """
            @Composable
            @GivenFun fun function(@Given string: String, assistedString: String) {
            }
            
            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testSuspendGivenFunMulti() = multiCodegen(
        listOf(
            source(
                """
                    @GivenFun
                    suspend fun function(string: String, @FunApi assistedString: String) {
                    }
                    
                    @GivenFun fun usage(function: function) {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component abstract class TestComponent(@Given val string: String) {
                        abstract val function: function
                        abstract val usage: usage
                    }
                """
            )
        )
    )

    @Test
    fun testGivenFunWithTypeParameters() = codegen(
        """
            @GivenFun inline fun <reified T : S, S> function(t: T): S {
                return error("")
            }

            fun invoke(@Given string: String) = given<function<String, String>>()
        """
    )

    @Test
    fun testGivenFunWithTypeParametersWithMultipleUpperBounds() = codegen(
        """
            @GivenFun inline fun <reified T, S> function(t: T): S where T : CharSequence, T : String {
                return error("")
            }

            fun invoke(@Given string: String) = given<function<String, String>>()
        """
    )

    @Test
    fun testComposableGivenFunWithComposableDependency() = codegen(
        """
            @GivenFun @Composable fun function(foo: Foo) {
            }
            @Given @Composable fun foo() = Foo()

            fun invoke() = given<function>()
        """
    )

    @Test
    fun testSuspendGivenFunWithSuspendDependency() = codegen(
        """
            @GivenFun
            suspend fun function(foo: Foo) {
            }
            @Given
            suspend fun foo() = Foo()
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testGivenFunDependencyGetsCreatedOnInvocation() = codegen(
        """
            @GivenFun fun function(foo: Foo) {
            }
            var fooCalled = false
            @Given fun foo() = Foo().also { fooCalled = true }
            
            fun invoke() {
                val component = component<TestComponent>()
                junit.framework.Assert.assertFalse(fooCalled)
                val function = given<function>()
                junit.framework.Assert.assertFalse(fooCalled)
                function()
                junit.framework.Assert.assertTrue(fooCalled)
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenFunWithNonExplicitAssistedParameters() = codegen(
        """
            @GivenFun fun function(foo: Foo) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: (Foo) -> function
            }
        """
    )

    @Test
    fun testInternalGivenFun() = multiCodegen(
        listOf(
            source(
                """
                    @GivenFun
                    internal fun function() {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Given 
                    fun usage(function: function) {
                    }
                """
            )
        )
    ) { (a, b) ->
        a.assertOk()
        b.assertCompileError("internal")
    }

    @Test
    fun testGivenFunWithFunApiDefaultValue() = codegen(
        """
            @GivenFun fun function(@Given string: String, assisted: String = "") {
            }

            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testGivenFunWithDependencyDefaultValue() = codegen(
        """
            @GivenFun fun function(string: String = "", @FunApi assisted: String) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testGivenFunWithNullableDependencyDefaultValue() = codegen(
        """
            @GivenFun fun function(@Given string: String?, assisted: String) {
            }

            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testSuspendGivenFunCanRequestSuspendDependencies() = codegen(
        """
            @Given 
            suspend fun foo() = Foo()
            
            @GivenFun
            suspend fun function(foo: Foo) {
            }
            
            fun invoke() = given<function>()
        """
    )

    @Test
    fun testComposableGivenFunCanRequestComposableDependencies() = codegen(
        """
            @Given
            @Composable
            fun foo() = Foo()
            
            @GivenFun
            @Composable
            fun function(foo: Foo) {
            }
            
            fun invoke() = given<function>()
        """
    )

    @Test
    fun testGivenFunExtension() = codegen(
        """
            @GivenFun fun function(string: String) {
            }
            
            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testGivenFunExtensionWithDefaultParameter() = codegen(
        """
            @GivenFun fun function(@Given string: String, param: Int = 0) {
            }
            
            fun invoke(@Given string: String) = given<function>()
        """
    )

    @Test
    fun testGivenFunWithGivenFunDependencyAndNullableParameter() = codegen(
        """
            typealias PrivacyPolicyUrl = String
            
            @GivenFun
            @Composable
            fun AboutPage(
                privacyPolicyUrl: PrivacyPolicyUrl?,
                aboutSection: AboutSection,
            ) {
            }
            
            @GivenFun
            @Composable
            fun AboutSection(
                privacyPolicyUrl: PrivacyPolicyUrl? = null
            ) {
            }
            
            fun invoke() = given<AboutPage>()
        """
    )

    @Test
    fun testGivenFunWithNonAmbiguousName() = codegen(
        """
            @GivenFun fun function(@Given string: String) {
            }
            @Given fun function() {
            }
        """
    ) {
        assertCompileError()
    }

    @Test
    fun testGivenFunWithoutExplicitReturnType() = codegen(
        """
            @GivenFun fun function() = 0
        """
    ) {
        assertCompileError()
    }

}
