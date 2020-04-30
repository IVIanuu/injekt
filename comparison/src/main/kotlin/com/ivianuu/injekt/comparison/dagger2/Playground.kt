package com.ivianuu.injekt.comparison.dagger2

import dagger.Component
import dagger.MembersInjector
import dagger.Module
import dagger.Subcomponent
import javax.inject.Inject
import javax.inject.Singleton

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
