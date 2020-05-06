package com.ivianuu.injekt.compiler

import io.github.classgraph.ClassGraph
import org.junit.Test
import java.io.File

class CommonScenariosTest {

    @Test
    fun testDifferentCompilations() {
        val compilation1 = compile {
            this.sources = listOf(
                source(
                    """
                    @Module
                    fun otherModule() {
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
            )
        }.also {
            it.assertOk()
        }

        val compilation2 = compile {
            this.sources = listOf(
                source(
                    """
                    @Module
                    fun thisModule() {
                        childFactory(::otherChildFactory)
                    }
                    
                    @Factory
                    fun thisFactory(): ThisComponent {
                        thisModule()
                        otherModule()
                        return createImpl()
                    }
                    
                    interface ThisComponent {
                        val annotated: OtherAnnotatedClass
                        val injector: @MembersInjector (OtherMembersInjectorTarget) -> Unit
                        val childFactory: @ChildFactory (OtherChildComponent) -> Unit
                    }
                """
                )
            )
            val classGraph = ClassGraph()
                .addClassLoader(compilation1.classLoader)
            val classpaths = classGraph.classpathFiles
            val modules = classGraph.modules.mapNotNull { it.locationFile }
            this.classpaths += (classpaths + modules).distinctBy(File::getAbsolutePath)
        }.also { it.assertOk() }
    }

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