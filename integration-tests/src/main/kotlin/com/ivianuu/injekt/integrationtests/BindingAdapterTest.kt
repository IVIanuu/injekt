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
            
            @Component
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
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
            
            @Component
            abstract class MyComponent {
                abstract val annotatedBar: AnnotatedBar
                
                @Binding protected fun foo() = Foo()
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

            @Component
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
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

            @Component
            abstract class MyComponent {
                abstract suspend fun any(): Any
                
                @Binding protected fun foo() = Foo()
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

            @Component
            abstract class MyComponent {
                @Composable
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
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
            
            @Component
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
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

            @Component
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
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
            
            @Component
            abstract class MyComponent {
                abstract val any: Any
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
            
            @Component
            abstract class MyComponent {
                abstract val uiComponents: Set<@Composable () -> Unit>
            }
            
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
                    
                    @Component
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

            @Component
            abstract class MyComponent {
                abstract val any: Any
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

            @Component
            abstract class MyComponent {
                abstract val any: Any
                @Binding protected fun foo() = Foo()
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

            @Component
            abstract class MyComponent {
                abstract val any: Any
                @Binding protected fun foo() = Foo()
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

            @Component
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
        """
    )

    @Test
    fun testBindingAdapterWithArrayArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Array<String>) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Array<String>, instance: T) = arg
                }
            }

            @MyAdapter(arrayOf("a"))
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Array<String>
            }
        """
    )

    @Test
    fun testBindingAdapterWithBooleanArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Boolean) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Boolean, instance: T) = arg
                }
            }

            @MyAdapter(true)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Boolean
            }
        """
    )

    @Test
    fun testBindingAdapterWithByteArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Byte) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Byte, instance: T) = arg
                }
            }

            @MyAdapter(0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Byte
            }
        """
    )

    @Test
    fun testBindingAdapterWithCharArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Char) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Char, instance: T) = arg
                }
            }

            @MyAdapter('a')
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Char
            }
        """
    )

    @Test
    fun testBindingAdapterWithDoubleArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Double) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Double, instance: T) = arg
                }
            }

            @MyAdapter(0.0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Double
            }
        """
    )

    @Test
    fun testBindingAdapterWithEnumArg() = codegen(
        """
            enum class MyEnum { A, B }
            
            @BindingAdapter
            annotation class MyAdapter(val arg: MyEnum) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: MyEnum, instance: T) = arg
                }
            }

            @MyAdapter(MyEnum.A)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: MyEnum
            }
        """
    )

    @Test
    fun testBindingAdapterWithFloatArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Float) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Float, instance: T) = arg
                }
            }

            @MyAdapter(0f)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Float
            }
        """
    )

    @Test
    fun testBindingAdapterWithIntArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Int) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Int, instance: T) = arg
                }
            }

            @MyAdapter(0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Int
            }
        """
    )

    @Test
    fun testBindingAdapterWithClassArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: KClass<*>) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: KClass<*>, instance: T) = arg
                }
            }

            @MyAdapter(Any::class)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: KClass<*>
            }
        """
    )

    @Test
    fun testBindingAdapterWithLongArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Long) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Long, instance: T) = arg
                }
            }

            @MyAdapter(0L)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Long
            }
        """
    )

    @Test
    fun testBindingAdapterWithShortArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: Short) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: Short, instance: T) = arg
                }
            }

            @MyAdapter(0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Short
            }
        """
    )

    @Test
    fun testBindingAdapterWithStringArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: String) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: String, instance: T) = arg
                }
            }

            @MyAdapter("my_name")
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: String
            }
        """
    )

    @Test
    fun testBindingAdapterWithUByteArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: UByte) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: UByte, instance: T) = arg
                }
            }

            @MyAdapter(0u)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: UByte
            }
        """
    )

    @Test
    fun testBindingAdapterWithUIntArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: UInt) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: UInt, instance: T) = arg
                }
            }

            @MyAdapter(0u)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: UInt
            }
        """
    )

    @Test
    fun testBindingAdapterWithULongArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: ULong) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: ULong, instance: T) = arg
                }
            }

            @MyAdapter(0UL)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: ULong
            }
        """
    )

    @Test
    fun testBindingAdapterWithUShortArg() = codegen(
        """
            @BindingAdapter
            annotation class MyAdapter(val arg: UShort) {
                companion object {
                    @Binding
                    fun <T> bindArg(@BindingAdapterArg("arg") arg: UShort, instance: T) = arg
                }
            }

            @MyAdapter(0u)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: UShort
            }
        """
    )

    @Test
    fun testBindingAdapterWithTypeArg() = codegen(
        """
            @BindingAdapter
            annotation class Alias<T> {
                companion object {
                    @Binding
                    fun <T, S : T> bindAlias(instance: S): T = instance
                }
            }

            @Alias<Any>
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Any
            }
        """
    )

}
