package com.ivianuu.injekt

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
                        return createImpl()
                    }
                    
                    @Transient
                    class OtherAnnotatedClass(foo: Foo)
                    
                    @Transient
                    class OtherAssistedClass(
                        @Assisted assisted: String,
                        foo: Foo
                    )
                    
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
                        return createImpl()
                    }
                    
                    interface ThisComponent {
                        val annotated: OtherAnnotatedClass
                        val assisted: @Provider (String) -> OtherAssistedClass
                        val injector: @MembersInjector (OtherMembersInjectorTarget) -> Unit
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
        
        @Transient class WorkerA(@Assisted context: Context, foo: Foo) : Worker(context)
        @Transient class WorkerB(@Assisted context: Context) : Worker(context)
        
        @Module 
        inline fun <T : Worker> bindWorkerIntoMap() {
            map<KClass<out Worker>, @Provider (Context) -> Worker> {
                put<@Provider (Context) -> T>(classOf<T>())
            }
        }
        
        @Transient
        class WorkerFactory(
            private val workers: Map<KClass<out Worker>, @Provider (Context) -> Worker>,
            private val context: Context
        )
        
        interface AppComponent {
            val injectApp: @MembersInjector (App) -> Unit
        }
        
        @Factory
        fun createAppComponent(): AppComponent {
            transient { Foo() }
            bindWorkerIntoMap<WorkerA>()
            bindWorkerIntoMap<WorkerB>()
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

    @Test
    fun viewModelFeature() = codegen(
        """
        abstract class ViewModel
        
        @Transient
        class ViewModelStore {
            fun <T : ViewModel> getOrCreate(clazz: Class<T>, factory: ViewModelFactory): T = factory.create(clazz)
            abstract class ViewModelFactory {
                abstract fun <T : ViewModel> create(clazz: Class<T>): T
            }
        }
        
        @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
        @Qualifier
        annotation class UnscopedViewModel
        
        @Module
        inline fun <T : ViewModel> viewModel() {
            val clazz = classOf<T>()
            transient<@UnscopedViewModel T>()
            transient {
                val viewModelStore = get<ViewModelStore>()
                val viewModelProvider = get<@Provider () -> @UnscopedViewModel T>()
                viewModelStore.getOrCreate<T>(clazz.java, object : ViewModelStore.ViewModelFactory() {
                    override fun <T : ViewModel> create(clazz: Class<T>): T =
                        viewModelProvider() as T
                })
            }
        }
        
        class MyViewModel : ViewModel()

        interface ViewModelComponent { 
            val myViewModel: MyViewModel
        }
        
        @Factory
        fun create(): ViewModelComponent {
            viewModel<MyViewModel>()
            return createImpl()
        }
        
        fun invoke() = create().myViewModel
    """
    ) {
        invokeSingleFile()
    }

}