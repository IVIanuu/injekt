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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class BindingAdapterTest {

    @Test
    fun testBindingAdapterWithClass() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding { 
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo)
            
            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithSuperTypeArgument() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding { 
                companion object {
                    @Binding
                    val <T> T.any: T get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo)
            
            @MergeComponent
            abstract class MyComponent {
                abstract val annotatedBar: AnnotatedBar
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithTopLevelFunction() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            fun myService(foo: Foo) {
            }

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithSuspendFunction() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            suspend fun myService(foo: Foo) {
            }

            @MergeComponent
            abstract class MyComponent {
                abstract suspend fun any(): Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithComposableFunction() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            @Composable
            fun myService(foo: Foo) {
            }

            @MergeComponent
            abstract class MyComponent {
                @Composable
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithFunctionInObject() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            object MyObject {
                @AnyBinding
                fun myService(foo: Foo) {
                }
            }
            
            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithFunBinding() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : () -> Unit> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            @FunBinding
            fun myService(foo: Foo) {
            }

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithAssistedFunBinding() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : (Foo) -> Unit> T.any: Any get() = this
                }
            }

            @AnyBinding
            @FunBinding
            fun myService(@FunApi foo: Foo) {
            }
            
            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testSetBindingAdapterWithComposableFunction() = codegen(
        """
            @BindingAdapter
            annotation class UiComponentBinding {
                companion object {
                    @SetElements
                    fun <T : @Composable () -> Unit> intoSet(instance: T): Set<@Composable () -> Unit> = setOf(instance)
                }
            }
            
            @UiComponentBinding
            @FunBinding
            @Composable
            fun MyUiComponent() {
            }
            
            @MergeComponent
            abstract class MyComponent {
                abstract val uiComponents: Set<@Composable () -> Unit>
            }
            
            @GenerateMergeComponents
            fun invoke() {
                val uiComponents = component<MyComponent>().uiComponents
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testSetBindingAdapterWithComposableFunctionMulti() = multiCodegen(
        listOf(
            source(
                """
                    @BindingAdapter
                    annotation class UiComponentBinding {
                        companion object {
                            @SetElements
                            fun <T : @Composable () -> Unit> intoSet(instance: T): Set<@Composable () -> Unit> = setOf(instance)
                        }
                    }
                    
                    @MergeComponent
                    abstract class MyComponent {
                        abstract val uiComponents: Set<@Composable () -> Unit>
                    }
                    
                    @UiComponentBinding
                    @FunBinding
                    @Composable
                    fun MyUiComponent() {
                    }
        """
            )
        ),
        listOf(
            source(
                """
                    @GenerateMergeComponents
                    fun invoke() {
                        val uiComponents = component<MyComponent>().uiComponents
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }

    @Test
    fun testBindingAdapterWithFunctionTypeAlias() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter {
                companion object {
                    @Binding
                    fun <T : () -> Unit> bind(instance: T): T = instance
                }
            }
            
            typealias MyFunction = () -> Unit
            
            @MyAdapter
            fun myFunction(): MyFunction = {}
        """
    )

    @Test
    fun testBindingAdapterOnPropertyBinding() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            val myBinding: Foo get() = Foo()

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterOnExtensionFunctionBinding() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            fun Foo.myBinding(): Bar = Bar(this)

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterOnExtensionPropertyBinding() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            val Foo.myBinding: Bar get() = Bar(this)

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingAdapterWithImplBinding() = codegen(
        """
            @BindingAdapter
            annotation class AnyBinding {
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            interface MyService
            
            @AnyBinding
            @ImplBinding
            class MyServiceImpl(foo: Foo) : MyService

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

}
