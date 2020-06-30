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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class CommonScenariosTest {

    @Test
    fun testDifferentCompilations() = multiCodegen(
        listOf(
            source(
                """
                    @Module
                    fun otherModule(instance: String) {
                        instance(instance)
                        transient { Foo() }
                        transient { CommandA() }
                        set<Command> {
                            add<CommandA>()
                        }
                        map<KClass<out Command>, Command> {
                            put<CommandA>(CommandA::class)
                        }
                    }
                    
                    @ChildFactory
                    fun otherChildFactory(): OtherChildComponent {
                        return create()
                    }
                    
                    @Transient
                    class OtherAnnotatedClass(foo: Foo)
                    
                    @Transient
                    class OtherAssistedClass(
                        assisted: @Assisted String,
                        foo: Foo
                    )
                    
                    interface OtherChildComponent {
                        val foo: Foo
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Module
                    fun thisModule() {
                        childFactory(::otherChildFactory)
                        transient { CommandB() }
                        set<Command> {
                            add<CommandB>()
                        }
                        map<KClass<out Command>, Command> {
                            put<CommandB>(CommandB::class)
                        }
                    }
                    
                    @Factory
                    fun thisFactory(instance: String): ThisComponent {
                        thisModule()
                        otherModule(instance)
                        return create()
                    }
                    
                    interface ThisComponent {
                        val annotated: OtherAnnotatedClass
                        val assisted: @Provider (String) -> OtherAssistedClass
                        val childFactory: @ChildFactory () -> OtherChildComponent
                        val commandSet: Set<Command>
                    }
                """
            )
        )
    )

    @Test
    fun testWorkerMapInApplicationScenario() = codegen(
        """
        abstract class Worker(context: Context)
        
        @Transient class Context
        
        @Transient class WorkerA(context: @Assisted Context, foo: Foo) : Worker(context)
        @Transient class WorkerB(context: @Assisted Context) : Worker(context)
        
        @Module 
        inline fun <reified T : Worker> bindWorkerIntoMap() {
            map<KClass<out Worker>, @Provider (Context) -> Worker> {
                put<@Provider (Context) -> T>(T::class)
            }
        }
        
        @Transient
        class WorkerFactory(
            private val workers: Map<KClass<out Worker>, @Provider (Context) -> Worker>,
            private val context: Context
        )
        
        interface AppComponent {
            val workerFactory: WorkerFactory
        }
        
        @Factory
        fun createAppComponent(): AppComponent {
            transient { Foo() }
            bindWorkerIntoMap<WorkerA>()
            bindWorkerIntoMap<WorkerB>()
            return create()
        }
        
        class App {  
            init {
                createAppComponent().workerFactory
            }
        }
        
        fun invoke() = App()
    """
    ) {
        invokeSingleFile()
    }

}
