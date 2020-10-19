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

class BindingModuleTest {

    @Test
    fun testBindingModuleWithClass() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
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
    fun testBindingModuleWithAssistedClass() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo)
            
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
    fun testBindingModuleWithTopLevelFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
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
    fun testBindingModuleWithSuspendFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
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
    fun testBindingModuleWithComposableFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
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
    fun testBindingModuleWithAssistedTopLevelFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            @AnyBinding
            fun myService(foo: Foo) {
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
    fun testBindingModuleWithFunctionInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
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
    fun testBindingModuleWithAssistedFunctionInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
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
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingModuleWithTopLevelFunBinding() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : () -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            @Binding
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
    fun testBindingModuleWithAssistedTopLevelFunBinding() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : (Foo) -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            @AnyBinding
            @Binding
            fun myService(foo: Foo) {
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
    fun testBindingModuleWithFunBindingInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : () -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            object MyObject {
                @AnyBinding
                @Binding
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
    fun testBindingModuleWithAssistedFunBindingInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : (Foo) -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            object MyObject {
                @AnyBinding
                @Binding
                fun myService(foo: Foo) {
                }
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
    fun testSetBindingModuleWithComposableFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class UiComponentBinding {
                @Module
                class Impl<T : @Composable () -> Unit> {
                    @SetElements
                    fun intoSet(instance: T): Set<@Composable () -> Unit> = setOf(instance)
                }
            }
            
            @UiComponentBinding
            @Binding
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
    fun testSetBindingModuleWithComposableFunctionMulti() = multiCodegen(
        listOf(
            source(
                """
                    @BindingModule(MyComponent::class)
                    annotation class UiComponentBinding {
                        @Module
                        class Impl<T : @Composable () -> Unit> {
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

}
