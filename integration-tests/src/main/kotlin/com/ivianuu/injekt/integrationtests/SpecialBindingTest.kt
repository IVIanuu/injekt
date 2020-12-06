/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class SpecialBindingTest {

    @Test
    fun testSimpleFunBinding() = codegen(
        """
            @FunBinding fun function(string: String) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSimpleTypeBinding() = codegen(
        """
            @TypeBinding
            fun isLoggedIn(string: String): Boolean = true
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
                abstract val isLoggedIn: isLoggedIn
            }
        """
    )

    @Test
    fun testFunBindingWithExtension() = codegen(
        """
            @FunBinding fun String.function() {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSimpleFunBindingInDifferentPackage() = codegen(
        source(
            """
                @FunBinding fun function(string: String) {
                }
            """
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function
                
                @Component abstract class TestComponent(@Binding val string: String) {
                    abstract val function: function
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt")
        )
    )

    @Test
    fun testSimpleFunBindingInDifferentPackageComplex() = codegen(
        source(
            """
                @FunBinding fun function(string: String) {
                }
            """
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function
                @FunBinding fun function2(function: function, string: String) {
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt2")
        ),
        source(
            """
                import com.ivianuu.injekt.integrationtests.function 
                import com.ivianuu.injekt2.function2

                @Component abstract class TestComponent(@Binding val string: String) {
                    abstract val function: function
                    abstract val function2: function2
                }
            """,
            packageFqName = FqName("com.ivianuu.injekt")
        )
    )

    @Test
    fun testAssistedFunBinding() = codegen(
        """
            @FunBinding fun function(string: String, @FunApi assisted: Int) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testAssistedFunBindingMulti() = multiCodegen(
        listOf(
            source(
                """
                    @FunBinding fun function(string: String, @FunApi assisted: Int) {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component abstract class TestComponent(@Binding val string: String) {
                        abstract val function: function
                    }
                """
            )
        )
    )

    @Test
    fun testAssistedExtensionFunBinding() = codegen(
        """
            @FunBinding fun @receiver:FunApi String.function(string: String) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testAssistedExtensionSuspendFunBinding() = codegen(
        """
            @FunBinding
            suspend fun @receiver:FunApi String.function(string: String) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSuspendFunBinding() = codegen(
        """
            @FunBinding
            suspend fun function(string: String, @FunApi assistedString: String) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testComposableFunBinding() = codegen(
        """
            @Composable
            @FunBinding fun function(string: String, @FunApi assistedString: String) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
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
                    suspend fun function(string: String, @FunApi assistedString: String) {
                    }
                    
                    @FunBinding fun usage(function: function) {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component abstract class TestComponent(@Binding val string: String) {
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
            @FunBinding inline fun <reified T : S, S> function(t: T): S {
                return error("")
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function<String, String>
            }
        """
    )

    @Test
    fun testFunBindingWithTypeParametersWithMultipleUpperBounds() = codegen(
        """
            @FunBinding inline fun <reified T, S> function(t: T): S where T : CharSequence, T : String {
                return error("")
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function<String, String>
            }
        """
    )

    @Test
    fun testComposableFunBindingWithComposableDependency() = codegen(
        """
            @FunBinding
            @Composable
            fun function(foo: Foo) {
            }

            @Binding
            @Composable
            fun foo() = Foo()
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSuspendFunBindingWithSuspendDependency() = codegen(
        """
            @FunBinding
            suspend fun function(foo: Foo) {
            }

            @Binding
            suspend fun foo() = Foo()
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testFunBindingDependencyGetsCreatedOnInvocation() = codegen(
        """
            @FunBinding fun function(foo: Foo) {
            }

            var fooCalled = false
            @Binding fun foo() = Foo().also { fooCalled = true }
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
            
            fun invoke() {
                val component = component<TestComponent>()
                junit.framework.Assert.assertFalse(fooCalled)
                val function = component.function
                junit.framework.Assert.assertFalse(fooCalled)
                function()
                junit.framework.Assert.assertTrue(fooCalled)
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testFunBindingWithNonExplicitAssistedParameters() = codegen(
        """
            @FunBinding fun function(foo: Foo) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: (Foo) -> function
            }
        """
    )

    @Test
    fun testInternalFunBinding() = multiCodegen(
        listOf(
            source(
                """
                    @FunBinding
                    internal fun function() {
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Binding 
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
    fun testFunBindingWithFunApiDefaultValue() = codegen(
        """
            @FunBinding fun function(string: String, @FunApi assisted: String = "") {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
            }
        """
    )

    @Test
    fun testFunBindingWithDependencyDefaultValue() = codegen(
        """
            @FunBinding fun function(string: String = "", @FunApi assisted: String) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testFunBindingWithNullableDependencyDefaultValue() = codegen(
        """
            @FunBinding fun function(string: String?, @FunApi assisted: String) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testSuspendFunBindingCanRequestSuspendDependencies() = codegen(
        """
            @Binding 
            suspend fun foo() = Foo()
            
            @FunBinding
            suspend fun function(foo: Foo) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testComposableFunBindingCanRequestComposableDependencies() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()
            
            @FunBinding
            @Composable
            fun function(foo: Foo) {
            }
            
            @Component abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testFunBindingExtension() = codegen(
        """
            @FunBinding fun function(string: String) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
                fun invoke() {
                    function.invokeFunction()
                }
            }
        """
    )

    @Test
    fun testFunBindingExtensionWithDefaultParameter() = codegen(
        """
            @FunBinding fun function(string: String, @FunApi param: Int = 0) {
            }
            
            @Component abstract class TestComponent(@Binding val string: String) {
                abstract val function: function
                fun invoke() {
                    function.invokeFunction()
                }
            }
        """
    )

    @Test
    fun testFunBindingWithFunBindingDependencyAndNullableParameter() = codegen(
        """
            typealias PrivacyPolicyUrl = String
            
            @FunBinding
            @Composable
            fun AboutPage(
                privacyPolicyUrl: PrivacyPolicyUrl?,
                aboutSection: AboutSection,
            ) {
            }
            
            @FunBinding
            @Composable
            fun AboutSection(
                @FunApi privacyPolicyUrl: PrivacyPolicyUrl? = null
            ) {
            }
            
            @Component abstract class MyComponent {
                abstract val aboutPage: AboutPage
            }
        """
    )
}
