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
    fun testSetEffectWithClass() = codegen(
        """
            interface MyInterface
            @Effect
            annotation class SetEffect {
                companion object {
                    @SetElements
                    fun <T : MyInterface> intoSet(instance: T): Set<MyInterface> = setOf(instance)
                }
            }
            
            @SetEffect
            class A(string: String) : MyInterface
            
            @Binding
            fun string() = ""
            
            @SetEffect
            class B(int: Int) : MyInterface
            
            @Binding
            fun int() = 0
            
            @Component
            abstract class MyComponent {
                abstract val set: Set<MyInterface>
            }
            
            fun invoke() {
                component<MyComponent>().set
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testSetEffectWithReturnedFunction() = codegen(
        """
            @Effect
            annotation class SetEffect {
                companion object {
                    @SetElements
                    fun <T : () -> Unit> intoSet(instance: T): Set<() -> Unit> = setOf(instance)
                }
            }
            
            @SetEffect
            fun SetEffectElement(string: String): () -> Unit = {
            }
            
            @Binding
            fun string() = ""
            
            @Component
            abstract class MyComponent {
                abstract val set: Set<() -> Unit>
            }
            
            fun invoke() {
                component<MyComponent>().set
            }
        """
    ) {
        invokeSingleFile()
    }

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
    fun testEffectWithArrayParam() = codegen(
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
    fun testEffectWithBooleanParam() = codegen(
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
    fun testEffectWithByteParam() = codegen(
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
    fun testEffectWithCharParam() = codegen(
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
    fun testEffectWithDoubleParam() = codegen(
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
    fun testEffectWithEnumParam() = codegen(
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
    fun testEffectWithFloatParam() = codegen(
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
    fun testEffectWithIntParam() = codegen(
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
    fun testEffectWithClassParam() = codegen(
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
    fun testEffectWithLongParam() = codegen(
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
    fun testEffectWithShortParam() = codegen(
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
    fun testEffectWithStringParam() = codegen(
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
    fun testEffectWithUByteParam() = codegen(
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
    fun testEffectWithUIntParam() = codegen(
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
    fun testEffectWithULongParam() = codegen(
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
    fun testEffectWithUShortParam() = codegen(
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
    fun testEffectWithTypeParam() = codegen(
        """
            @Effect
            annotation class Alias<T> {
                companion object {
                    @Binding
                    fun <@Arg("T") T, S : T> bindAlias(instance: S): T = instance
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
            typealias UiDecorators = Set<UiDecorator>
            data class UiDecorator(
                val key: String,
                val dependencies: Set<String>,
                val dependents: Set<String>,
                val content: @Composable (@Composable () -> Unit) -> Unit
            )
            
            @Effect
            annotation class UiDecoratorBinding(
                val key: String,
                val dependencies: Array<String> = [],
                val dependents: Array<String> = []
            ) {
                companion object {
                    @SetElements
                    fun <T : @Composable (@Composable () -> Unit) -> Unit> uiDecoratorIntoSet(
                        @Arg("key") key: String,
                        @Arg("dependencies") dependencies: Array<String>?,
                        @Arg("dependents") dependents: Array<String>?,
                        content: T
                    ): UiDecorators = setOf(UiDecorator(
                        key = key,
                        dependencies = dependencies?.toSet() ?: emptySet(),
                        dependents = dependents?.toSet() ?: emptySet(),
                        content = content as @Composable (@Composable () -> Unit) -> Unit
                    ))
                }
            }
                        
            @Effect
            annotation class AppThemeBinding {
                companion object {
                    @UiDecoratorBinding("app_theme", dependencies = ["system_bars"])
                    fun <T : @Composable (@Composable () -> Unit) -> Unit> uiDecorator(
                        instance: T
                    ) = instance
                }
            }

            @AppThemeBinding
            @FunBinding
            @Composable
            fun SampleTheme(@FunApi children: @Composable () -> Unit) {
                children()
            }

            @Component
            abstract class MyComponent {
                abstract val uiDecorators: UiDecorators
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
    fun testEffectWithSuperTypeTypeParameterInference() = codegen(
        """ 
            @MapBindings
            @Binding
            fun map(): MutableMap<String, Int> = error("")

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
                abstract val mutableMap: MutableMap<String, Int>
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
                    fun <@Arg("M") M : Map<K, V>, T, K, V> state(): K = error("")
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
                    fun <@Arg("M") M : Map<K, V>, T : Set<E>, K, V, E> state(): Pair<E, K> = error("")
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
