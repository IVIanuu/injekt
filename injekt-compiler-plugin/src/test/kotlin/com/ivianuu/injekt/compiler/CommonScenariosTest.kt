package com.ivianuu.injekt.compiler

import org.junit.Test

class CommonScenariosTest {

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