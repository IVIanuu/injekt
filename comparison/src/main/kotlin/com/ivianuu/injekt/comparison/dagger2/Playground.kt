package com.ivianuu.injekt.comparison.dagger2

import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import dagger.Component
import dagger.Lazy
import javax.inject.Provider

@Component
interface LazyAndProvider {
    val fib4: Lazy<Fib4>
    val fib5: Provider<Fib5>
    val fib6: Provider<Fib6>
}

/**
@Singleton
@Component(modules = [ParentModule::class])
interface ParentComponent {
    val foo: Foo
    val childComponentFactory: ChildComponent.Factory
}

@Module(subcomponents = [ChildComponent::class])
class ParentModule

@Subcomponent
interface ChildComponent {
    val bar: Bar
    fun inject(myClass: MyClass)
    val injector: MembersInjector<MyClass>

    @Subcomponent.Factory
    interface Factory {
        fun create(): ChildComponent
    }
}

@Singleton
class Foo @Inject constructor()

class Bar @Inject constructor(val foo: Foo)

class MyClass {
    @Inject
    lateinit var foo: Foo
    @Inject
    lateinit var bar: Bar
}
 */