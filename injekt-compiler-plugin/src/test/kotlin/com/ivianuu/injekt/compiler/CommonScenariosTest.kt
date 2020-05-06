package com.ivianuu.injekt.compiler

import org.junit.Test

class CommonScenariosTest {

    @Test
    fun testDifferentCompilations() = multiCompilationCodegen(
        listOf(
            source(
                """
                    @Module
                    fun otherModule(instance: String) {
                        instance(instance)
                        transient { Foo() }
                    }
                    
                    @ChildFactory
                    fun otherChildFactory(): OtherChildComponent {
                        return createImpl()
                    }
                    
                    @Transient
                    class OtherAnnotatedClass(foo: Foo)
                    
                    class OtherMembersInjectorTarget {
                        @Inject lateinit var foo: Foo
                    }
                    
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
                    }
                    
                    @Factory
                    fun thisFactory(instance: String): ThisComponent {
                        thisModule()
                        otherModule(instance)
                        return createImpl()
                    }
                    
                    interface ThisComponent {
                        val annotated: OtherAnnotatedClass
                        val injector: @MembersInjector (OtherMembersInjectorTarget) -> Unit
                        val childFactory: @ChildFactory () -> OtherChildComponent
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
        
        @Transient class WorkerA(@Assisted context: Context, foo: Foo) : Worker(context)
        @Transient class WorkerB(@Assisted context: Context) : Worker(context)
        
        @Transient
        class WorkerFactory(
            private val workers: Map<String, @Provider (Context) -> Worker>,
            private val context: Context
        ) {
            init {
                println(workers)
                println(workers.mapValues { it.value(context) })
            }
        }
        
        interface AppComponent {
            val injectApp: @MembersInjector (App) -> Unit
        }
        
        @Factory
        fun createAppComponent(): AppComponent {
            transient { Foo() }
            map<String, @Provider (Context) -> Worker> {
                put<@Provider (Context) -> WorkerA>("a")
                put<@Provider (Context) -> WorkerB>("b")
            }
            return createImpl()
        }
        
        class App {
            @Inject private lateinit var workerFactory: WorkerFactory
            
            init {
                createAppComponent().injectApp(this)
            }
        }
        
        fun invoke() = App()
    """
    ) {
        invokeSingleFile()
    }

}