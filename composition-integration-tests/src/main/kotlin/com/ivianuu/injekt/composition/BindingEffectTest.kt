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

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
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
                    transient { t: T -> t.toString() }
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
        @Transient
        class Dep
        
        fun invoke() {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            component.runReading { 
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
                inline fun <reified T : AppService> bindAppService() { 
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
            val appServices = component.runReading { get<Map<KClass<AppService>, AppService>>() }
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
                        inline fun <reified T : AppService> bind() { 
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
                    val appServices = component.runReading {
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
                    transient<@ForActivity ViewModelStoreOwner> { Any() as ViewModelStoreOwner }
                    return create()
                }
                
                @Target(AnnotationTarget.TYPE)
                @Qualifier
                annotation class ForActivity
                
                @BindingAdapter(ActivityComponent::class)
                annotation class ActivityViewModel {
                    companion object { 
                        @Module 
                        inline fun <reified T : ViewModel> bind() {
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
                    transient<@UnscopedViewModel T>() 
                    transient { viewModelStoreOwner: S, viewModelProvider: @Provider () -> @UnscopedViewModel T ->
                        ViewModelProvider(
                            viewModelStoreOwner,
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
                    component.runReading { get<MainViewModel>() }
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
                fun <T> func() {
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
                inline fun <T> bind() {
                }
            }
        }
    """
    )

    // todo @Test
    fun testBindingAdapterWithoutCompanion() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
    """
    ) {
        assertCompileError("companion")
    }

    // todo @Test
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
                fun bind() {
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
                fun <A, B> bind() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testBindingAdapterWithTransient() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
        
        @BindingAdapterFunction(MyBindingAdapter::class)
        @Module
        fun <T> bind() {
        }
        
        @MyBindingAdapter
        @Transient
        class MyClass
    """
    ) {
        assertCompileError("transient")
    }

    @Test
    fun testBindingEffectWithTransient() = codegen(
        """
        @BindingEffect(TestCompositionComponent::class)
        annotation class MyBindingEffect {
            companion object {
                @Module
                fun <T> bind() {
                }
            }
        }
        
        @MyBindingEffect
        @Transient
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
                fun <T> bind() {
                }
            }
        }
        
        @Scoped<TestCompositionComponent>
        @MyBindingEffect
        class MyClass
    """
    ) {
        assertOk()
    }

    // todo @Test
    fun testBindingEffectNotInBounds() = codegen(
        """
        @BindingAdapter(TestCompositionComponent::class)
        annotation class MyBindingAdapter
        
        @BindingAdapterFunction(MyBindingAdapter::class)
        @Module 
        fun <T : UpperBound> bind() {
        }
        
        interface UpperBound
        
        @MyBindingAdapter
        class MyClass
    """
    ) {
        assertCompileError("bound")
    }

}
