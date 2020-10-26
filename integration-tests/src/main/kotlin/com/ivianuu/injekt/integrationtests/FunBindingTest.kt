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

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
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
            inline fun <reified T : S, S> function(t: T): S {
                return error("")
            }
            
            @Component
            abstract class TestComponent(@Binding val string: String) {
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
            
            @Component
            abstract class TestComponent {
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
            
            @Component
            abstract class TestComponent {
                abstract val function: function
            }
        """
    )

    @Test
    fun testFunBindingDependencyGetsCreatedOnInvocation() = codegen(
        """
            @FunBinding
            fun function(foo: Foo) {
            }

            var fooCalled = false
            @Binding
            fun foo() = Foo().also { fooCalled = true }
            
            @Component
            abstract class TestComponent {
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
    fun testFunBindingWithBindingAdapterDependencyGetsCreatedOnInvocation() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter {
                companion object {
                    @Binding
                    fun <T> alias(value: T):T = value
                }
            }
            
            @MyAdapter
            @FunBinding
            fun function(foo: Foo) {
            }

            var fooCalled = false
            @Binding
            fun foo() = Foo().also { fooCalled = true }
            
            @Component
            abstract class TestComponent {
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
            @FunBinding
            fun function(foo: Foo) {
            }
            
            @Component
            abstract class TestComponent {
                abstract val function: (Foo) -> function
            }
        """
    )

    @Test
    fun testFunBindingWithBindingAdapterWithNonExplicitAssistedParameters() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter {
                companion object {
                    @Binding
                    fun <T, P1> withoutFooArg(value: (P1) -> T): T {
                        return value(Foo() as P1)
                    }
                }
            }

            @MyAdapter
            @FunBinding
            fun function(foo: Foo) {
            }
            
            @Component
            abstract class TestComponent {
                abstract val function: function
            }
        """
    )

}
