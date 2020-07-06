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

package com.ivianuu.injekt.composition

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert
import junit.framework.Assert.assertTrue
import org.junit.Test

class BindingEffectTest {

    @Test
    fun testSimpleBindingEffect() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent { 
            return create() 
        }
        
        @BindingEffect(TestCompositionComponent::class)
        annotation class Effect1 {
            companion object {
                @Module 
                fun <T> bind1() { 
                    unscoped { get<T>().toString() }
                }
            }
        }
        
        @BindingEffect(TestCompositionComponent::class)
        annotation class Effect2 {
            companion object {
                @Module 
                fun <T : Any> bind2() { 
                    alias<T, Any>()
                }
            }
        }

        @Effect1
        @Effect2
        @Unscoped
        class Dep
        
        fun invoke() {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            component.runReader { 
                get<Dep>() 
                get<String>()
                get<Any>()
            }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testSimpleBindingAdapter() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent { 
            return create() 
        }

        interface AppService
        
        @BindingAdapter(TestCompositionComponent::class) 
        annotation class BindAppService {
            companion object {
                @Module 
                inline operator fun <reified T : AppService> invoke() { 
                    scoped<T>()
                    map<KClass<out AppService>, AppService> { 
                        put<T>(T::class)
                    }
                }
            }
        }

        @BindAppService 
        class MyAppServiceA : AppService
        
        @BindAppService 
        class MyAppServiceB : AppService

        fun invoke() {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            val appServices = component.runReader { get<Map<KClass<AppService>, AppService>>() }
            println("app services " + appServices)
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMultiCompilationBindingAdapter() =
        multiCodegen(
            listOf(
                source(
                    """
                @CompositionFactory 
                fun factory(): TestCompositionComponent { 
                    return create() 
                }

                interface AppService
        
                @BindingAdapter(TestCompositionComponent::class) 
                annotation class BindAppService {
                    companion object {
                        @Module 
                        inline operator fun <reified T : AppService> invoke() { 
                            scoped<T>()
                            map<KClass<out AppService>, AppService> { 
                                put<T>(T::class) 
                            }
                        } 
                    }
                }
        """
                ),
                source(
                    """
                @BindAppService 
                class MyAppServiceA : AppService
                
                @BindAppService 
                class MyAppServiceB : AppService
                
                fun invoke() { 
                    initializeCompositions() 
                    val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()() 
                    val appServices = component.runReader {
                        get<Map<KClass<AppService>, AppService>>()
                    }
                    println("app services " + appServices) 
                }
            """
                )
            )
        )

    @Test
    fun testMultiCompileViewModel() = multiCodegen(
        listOf(
            source(
                """
                abstract class ViewModel
                interface ViewModelStoreOwner
                class ViewModelStore
                class ViewModelProvider(
                    viewModelStoreOwner: ViewModelStoreOwner,
                    factory: Factory
                ) {
                    fun <T : ViewModel> get(clazz: Class<T>): T = injektIntrinsic()
                    
                    interface Factory { 
                        fun <T : ViewModel> create(clazz: Class<T>): T
                    }
                }
            """
            )
        ),
        listOf(
            source(
                """
                @CompositionComponent
                interface ActivityComponent
                
                @CompositionFactory
                fun createActivityComponent(): ActivityComponent { 
                    unscoped<@ForActivity ViewModelStoreOwner> { Any() as ViewModelStoreOwner }
                    return create()
                }
                
                @Target(AnnotationTarget.TYPE)
                @Qualifier
                annotation class ForActivity
                
                @BindingAdapter(ActivityComponent::class)
                annotation class ActivityViewModel {
                    companion object { 
                        @Module 
                        inline operator fun <reified T : ViewModel> invoke() {
                            activityViewModel<T>()
                        }
                    }
                }

                @Module
                inline fun <reified T : ViewModel> activityViewModel() { 
                    baseViewModel<T, @ForActivity ViewModelStoreOwner>()
                }
                
                @Module
                inline fun <reified T : ViewModel, S : ViewModelStoreOwner> baseViewModel() { 
                    unscoped<@UnscopedViewModel T>() 
                    unscoped {
                        val viewModelProvider = get<@Provider () -> @UnscopedViewModel T>()
                        ViewModelProvider(
                            get<S>(),
                            object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    viewModelProvider() as T
                            }
                        ).get(T::class.java) 
                    } 
                }
                
                @Target(AnnotationTarget.TYPE)
                @Qualifier 
                private annotation class UnscopedViewModel
                """
            )
        ),
        listOf(
            source(
                """
                @ActivityViewModel 
                class MainViewModel : ViewModel()
                
                fun run() {
                    initializeCompositions()
                    val component = compositionFactoryOf<ActivityComponent, () -> ActivityComponent>()()
                    component.runReader { get<MainViewModel>() }
                }
            """
            )
        )
    )

    @Test
    fun testBindingAdapterWithInvalidComponent() =
        codegen(
            """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module 
                operator fun <T> invoke() {
                }
            }
        }
    """
        ) {
            assertCompileError("@CompositionComponent")
        }

    @Test
    fun testCorrectBindingAdapter() = codegen(
        """
        @BindingAdapter(TestCompositionComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                inline operator fun <T> invoke() {
                }
            }
        }
    """
    )

    @Test
    fun testBindingAdapterWithoutCompanion() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
    """
    ) {
        assertCompileError("companion")
    }

    @Test
    fun testBindingAdapterWithoutModule() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object
        }
    """
    ) {
        assertCompileError("module")
    }

    @Test
    fun testBindingAdapterWithoutTypeParameters() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                operator fun invoke() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testBindingAdapterWithMultipleTypeParameters() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                operator fun <A, B> invoke() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testBindingAdapterWithUnscoped() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
        
        @BindingAdapterFunction(MyBindingAdapter::class)
        @Module
        fun <T> bind() {
        }
        
        @MyBindingAdapter
        @Unscoped
        class MyClass
    """
    ) {
        assertCompileError("unscoped")
    }

    @Test
    fun testBindingAdapterWithFunction() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @BindingAdapterFunction(MyBindingAdapter::class)
                @Module
                fun <T> bind() {
                }
            }
        }
        
        @MyBindingAdapter
        fun myFun() {
        }
    """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testBindingEffectWithUnscoped() = codegen(
        """
        @BindingEffect(TestCompositionComponent::class)
        annotation class MyBindingEffect {
            companion object {
                @Module
                operator fun <T> invoke() {
                }
            }
        }
        
        @MyBindingEffect
        @Unscoped
        class MyClass
    """
    ) {
        assertOk()
    }

    @Test
    fun testBindingAdapterWithScoped() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
        
        @BindingAdapterFunction(MyBindingAdapter::class)
        @Module 
        fun <T> bind() {
        }
        
        @TestScope
        @MyBindingAdapter
        class MyClass
    """
    ) {
        assertCompileError("scope")
    }

    @Test
    fun testBindingEffectWithScoped() = codegen(
        """
        @BindingEffect(TestCompositionComponent::class)
        annotation class MyBindingEffect {
            companion object { 
                @Module
                operator fun <T> invoke() {
                }
            }
        }
        
        @Scoped(TestCompositionComponent::class)
        @MyBindingEffect
        class MyClass
    """
    ) {
        assertOk()
    }

    @Test
    fun testBindingEffectNotInBounds() = codegen(
        """
        @BindingAdapter(TestCompositionComponent::class)
        annotation class MyBindingAdapter {
            companion object { 
                @Module 
                fun <T : UpperBound> bind() {
                }
            }
        }
        
        interface UpperBound
        
        @MyBindingAdapter
        class MyClass
    """
    ) {
        assertCompileError("bound")
    }

    @Test
    fun testFunctionBindingEffectNotInBounds() = codegen(
        """
        @BindingAdapter(TestCompositionComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @BindingAdapterFunction(MyBindingAdapter::class)
                @Module 
                fun <T : () -> Unit> bind() {
                }
            }
        }

        @MyBindingAdapter
        fun myFun(p0: String) {
        }
    """
    ) {
        assertCompileError("bound")
    }


    @Test
    fun testBindingEffectWithTypeParameters() = codegen(
        """
        @BindingAdapter(TestCompositionComponent::class)
        annotation class MyBindingAdapter {
            companion object { 
                @Module 
                fun <T> bind() {
                }
            }
        }

        @MyBindingAdapter
        class MyClass<T>
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testFunctionBindingEffect() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            return create() 
        }
        
        typealias FooFactory = () -> Foo
        
        @BindingEffect(TestCompositionComponent::class)
        annotation class BindFooFactory {
            companion object {
                @Module
                operator fun <T : FooFactory> invoke() {
                    alias<T, FooFactory>()
                }
            }
        }
        
        @BindFooFactory
        @Reader
        fun fooFactory(): Foo {
            return Foo()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { get<FooFactory>()() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendFunctionBindingEffect() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            return create() 
        }
        
        typealias FooFactory = suspend () -> Foo
        
        @BindingEffect(TestCompositionComponent::class)
        annotation class BindFooFactory {
            companion object {
                @Module
                operator fun <T : FooFactory> invoke() {
                    alias<T, FooFactory>()
                }
            }
        }
        
        @BindFooFactory
        @Reader
        suspend fun fooFactory(): Foo {
            return Foo()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { runBlocking { get<FooFactory>()() } }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

}
