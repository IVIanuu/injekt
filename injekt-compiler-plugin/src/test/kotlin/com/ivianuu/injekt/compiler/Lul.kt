package com.ivianuu.injekt.compiler

import org.junit.Test

class Lul {

    @Test
    fun test() = codegen(
        """
        abstract class Worker(context: Context)
        
        @Transient class Context
        
        class WorkerA(context: Context, foo: Foo) : Worker(context)
        class WorkerB(context: Context) : Worker(context)
        
        @Transient
        class WorkerFactory(
            private val workers: Map<String, @Provider (Context) -> Worker>,
            private val context: Context
        ) {
            init {
                println(workers.mapValues { it.value(context) })
            }
        }
        
        interface AppComponent {
            val injectApp: @MembersInjector (App) -> Unit
        }
        
        @Factory
        fun createAppComponent(): AppComponent = createImplementation {
            transient { Foo() }
            map<String, @Provider (Context) -> Worker> {
                put<@Provider (Context) -> WorkerA>("a")
                put<@Provider (Context) -> WorkerB>("b")
            }
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