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

package com.ivianuu.injekt

import org.junit.Test

class BindingAdapterTest {

    @Test
    fun testSimpleBindingAdapter() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent { 
            return createImpl() 
        }

        interface AppService
        
        @BindingAdapter(TestComponent::class) 
        annotation class BindAppService { 
            companion object { 
                @Module
                inline fun <T : AppService> bind() { 
                    scoped<T>()
                    map<KClass<out AppService>, AppService> { 
                        put<T>(classOf<T>())
                    }
                } 
            }
        }

        @BindAppService 
        class MyAppServiceA : AppService
        
        @BindAppService 
        class MyAppServiceB : AppService

        fun invoke() {
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val appServices = component.get<Map<KClass<AppService>, AppService>>()
            println("app services " + appServices)
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMultiCompilationBindingAdapter() = multiCodegen(
        listOf(
            source(
                """
                @CompositionFactory 
                fun factory(): TestComponent { 
                    return createImpl() 
                }

                interface AppService
        
                @BindingAdapter(TestComponent::class) 
                annotation class BindAppService { 
                companion object { 
                    @Module
                    inline fun <T : AppService> bind() { 
                        scoped<T>()
                        map<KClass<out AppService>, AppService> { 
                            put<T>(classOf<T>())
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
                    generateCompositions() 
                    val component = compositionFactoryOf<TestComponent, () -> TestComponent>()() 
                    val appServices = component.get<Map<KClass<AppService>, AppService>>()
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
                interface ActivityComponent
                
                @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
                @Qualifier
                annotation class ForActivity
                
                @BindingAdapter(ActivityComponent::class)
                annotation class ActivityViewModel { 
                    companion object { 
                        @Module
                        inline fun <T : ViewModel> bind() { 
                            activityViewModel<T>() 
                        } 
                    }
                }
                
                @Module
                inline fun <T : ViewModel> activityViewModel() { 
                    baseViewModel<T, @ForActivity ViewModelStoreOwner>()
                }
                
                @Module
                inline fun <T : ViewModel, S : ViewModelStoreOwner> baseViewModel() { 
                    transient<@UnscopedViewModel T>() 
                    val clazz = classOf<T>()
                    transient { 
                        val viewModelStoreOwner = get<S>() 
                        val viewModelProvider = get<@Provider () -> @UnscopedViewModel T>()
                        ViewModelProvider(
                            viewModelStoreOwner,
                            object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    viewModelProvider() as T
                                    }
                        ).get(clazz.java) 
                    } 
                }
                
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
            """
            )
        )
    )

}
