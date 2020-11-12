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
import org.junit.Assert.assertSame
import org.junit.Test

class EffectTest {

    @Test
    fun testEffectWithClass() = codegen(
        """
            @Effect
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
    fun testEffectWithTopLevelFunction() = codegen(
        """
            @Effect
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
    fun testEffectWithSuspendFunction() = codegen(
        """
            @Effect
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
    fun testEffectWithComposableFunction() = codegen(
        """
            @Effect
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
    fun testEffectWithFunctionInObject() = codegen(
        """
            @Effect
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
    fun testEffectWithFunBinding() = codegen(
        """
            @Effect
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
    fun testEffectWithAssistedFunBinding() = codegen(
        """
            @Effect
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
    fun testSetEffectWithComposableFunction() = codegen(
        """
            @Effect
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
    fun testSetEffectWithComposableFunctionMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Effect
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
    fun testEffectWithFunctionTypeAlias() = codegen(
        """
            @Effect
            annotation class MyEffect {
                companion object {
                    @Binding
                    fun <T : () -> Unit> bind(instance: T): T = instance
                }
            }
            
            typealias MyFunction = () -> Unit
            
            @MyEffect
            fun myFunction(): MyFunction = {}
        """
    )

    @Test
    fun testEffectOnPropertyBinding() = codegen(
        """
            @Effect
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
    fun testEffectOnExtensionFunctionBinding() = codegen(
        """
            @Effect
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
    fun testEffectOnExtensionPropertyBinding() = codegen(
        """
            @Effect
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
    fun testEffectWithImplBinding() = codegen(
        """
            @Effect
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
    fun testScopedEffectFunction() = codegen(
        """
            @Effect
            annotation class AnyBinding { 
                companion object {
                    @Binding(MyComponent::class)
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
            
            private val component = component<MyComponent>()
            
            fun invoke(): Pair<Any, Any> {
                return component.any to component.any
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testSuspendEffectFunction() = codegen(
        """
            @Effect
            annotation class AnyBinding { 
                companion object {
                    @Binding
                    suspend fun <T : Any> T.any(): Any = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo)
            
            @Component
            abstract class MyComponent {
                abstract suspend fun any(): Any
                
                @Binding protected fun foo() = Foo()
            }
        """
    )

    @Test
    fun testComposableEffectFunction() = codegen(
        """
            @Effect
            annotation class AnyBinding { 
                companion object {
                    @Binding
                    @Composable
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo)
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
        """
    )

    @Test
    fun testEffectWithArrayArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Array<String>) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Array<String>, instance: T) = arg
                }
            }

            @MyEffect(arrayOf("a"))
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Array<String>
            }
        """
    )

    @Test
    fun testEffectWithBooleanArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Boolean) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Boolean, instance: T) = arg
                }
            }

            @MyEffect(true)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Boolean
            }
        """
    )

    @Test
    fun testEffectWithByteArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Byte) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Byte, instance: T) = arg
                }
            }

            @MyEffect(0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Byte
            }
        """
    )

    @Test
    fun testEffectWithCharArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Char) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Char, instance: T) = arg
                }
            }

            @MyEffect('a')
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Char
            }
        """
    )

    @Test
    fun testEffectWithDoubleArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Double) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Double, instance: T) = arg
                }
            }

            @MyEffect(0.0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Double
            }
        """
    )

    @Test
    fun testEffectWithEnumArg() = codegen(
        """
            enum class MyEnum { A, B }
            
            @Effect
            annotation class MyEffect(val arg: MyEnum) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: MyEnum, instance: T) = arg
                }
            }

            @MyEffect(MyEnum.A)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: MyEnum
            }
        """
    )

    @Test
    fun testEffectWithFloatArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Float) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Float, instance: T) = arg
                }
            }

            @MyEffect(0f)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Float
            }
        """
    )

    @Test
    fun testEffectWithIntArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Int) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Int, instance: T) = arg
                }
            }

            @MyEffect(0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Int
            }
        """
    )

    @Test
    fun testEffectWithClassArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: KClass<*>) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: KClass<*>, instance: T) = arg
                }
            }

            @MyEffect(Any::class)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: KClass<*>
            }
        """
    )

    @Test
    fun testEffectWithLongArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Long) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Long, instance: T) = arg
                }
            }

            @MyEffect(0L)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Long
            }
        """
    )

    @Test
    fun testEffectWithShortArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: Short) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: Short, instance: T) = arg
                }
            }

            @MyEffect(0)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: Short
            }
        """
    )

    @Test
    fun testEffectWithStringArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: String) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: String, instance: T) = arg
                }
            }

            @MyEffect("my_name")
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: String
            }
        """
    )

    @Test
    fun testEffectWithUByteArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: UByte) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: UByte, instance: T) = arg
                }
            }

            @MyEffect(0u)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: UByte
            }
        """
    )

    @Test
    fun testEffectWithUIntArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: UInt) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: UInt, instance: T) = arg
                }
            }

            @MyEffect(0u)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: UInt
            }
        """
    )

    @Test
    fun testEffectWithULongArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: ULong) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: ULong, instance: T) = arg
                }
            }

            @MyEffect(0UL)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: ULong
            }
        """
    )

    @Test
    fun testEffectWithUShortArg() = codegen(
        """
            @Effect
            annotation class MyEffect(val arg: UShort) {
                companion object {
                    @Binding
                    fun <T> bindArg(@Arg("arg") arg: UShort, instance: T) = arg
                }
            }

            @MyEffect(0u)
            class MyService

            @Component
            abstract class MyComponent {
                abstract val arg: UShort
            }
        """
    )

    @Test
    fun testEffectWithTypeArg() = codegen(
        """
            @Effect
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

    @Test
    fun testEffectUsesAnotherEffect() = codegen(
        """
            @Effect
            annotation class A {
                companion object {
                    @Binding
                    fun <T : CharSequence> asCharSequence(instance: T): CharSequence = instance
                    @B
                    fun <T : CharSequence> b(instance: T): CharSequence = instance
                }
            }
            
            @Effect
            annotation class B {
                companion object {
                    @Binding
                    fun <T : Any> any(instance: T): Any = instance
                }
            }

            @A
            @Binding
            fun string() = "hello"

            @Component
            abstract class MyComponent {
                abstract val string: String
                abstract val charSequence: CharSequence
                abstract val any: Any
            }
        """
    )

    @Test
    fun testEffectWithTypeAliasTypeParameterInference() = codegen(
        """
            typealias StringIntMap = Map<String, Int>
            
            @MapBindings
            @Binding
            fun map(): StringIntMap = mapOf("a" to 0)

            @Effect
            annotation class MapBindings {
                companion object {
                    @Binding
                    val <T : Map<K, V>, K, V> T.map: Map<K, V> get() = this
                
                    @Binding
                    val <T : Map<K, V>, K, V> T.firstKey: K get() = keys.first()
                    
                    @Binding
                    val <T : Map<K, V>, K, V> T.firstValue: V get() = values.first()
                }
            }

            @Component
            abstract class MyComponent {
                abstract val key: String
                abstract val value: Int
                abstract val map: Map<String, Int>
                abstract val aliasedMap: StringIntMap
            }
        """
    )

    @Test
    fun testEffectWithTypeParameters() = codegen(
        """
            interface Store<S, A>
            
            @Effect
            annotation class StoreBinding {
                companion object {
                    @Binding
                    fun <T : Store<S, A>, S, A> state(instance: T): S = error("")
                }
            }
            
            @StoreBinding
            fun myStore(): Store<String, Int> = error("")

            @Component
            abstract class MyComponent {
                abstract val string: String
            }
        """
    )

    @Test
    fun testEffectTypeParameterInference() = codegen(
        """
            @Effect
            annotation class MapBinding<M : Map<*, *>> {
                companion object {
                    @Binding
                    fun <M : Map<K, V>, T, K, V> state(): K = error("")
                }
            }
            
            @MapBinding<Map<String, Int>>
            fun myStore(): Unit = error("")

            @Component
            abstract class MyComponent {
                abstract val string: String
            }
        """
    )

    @Test
    fun testEffectBindingAndAnnotationTypeParameterInference() = codegen(
        """
            @Effect
            annotation class MapBinding<M : Map<*, *>> {
                companion object {
                    @Binding
                    fun <M : Map<K, V>, T : Set<E>, K, V, E> state(): Pair<E, K> = error("")
                }
            }
            
            @MapBinding<Map<String, Int>>
            fun myStore(): Set<Int> = error("")

            @Component
            abstract class MyComponent {
                abstract val pair: Pair<Int, String>
            }
        """
    )

    @Test
    fun testEffectWithDefaultValueDependency() = codegen(
        """
            @Effect
            annotation class AnyBinding { 
                companion object {
                    @Binding
                    val <T : Any> T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo = Foo())
            
            @Component
            abstract class MyComponent {
                abstract val any: Any
            }
        """
    )

}
