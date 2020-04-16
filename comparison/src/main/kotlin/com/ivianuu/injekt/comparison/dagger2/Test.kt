package com.ivianuu.injekt.comparison.dagger2

import dagger.Component
import dagger.Module
import dagger.Provides

@Component(modules = [DepM::class])
interface DepComponent {
    val string: String
}

@Module
object DepM {
    @Provides
    fun string() = ""
}

@Component(dependencies = [DepComponent::class], modules = [MyModule::class])
interface ConsumerComponent {
    val integer: Int

    @Component.Factory
    interface Factory {
        fun create(
            depComponent: DepComponent,
            myModule: MyModule
        ): ConsumerComponent
    }
}

@Module
class MyModule(private val lol: String) {
    @Provides
    fun provide(string: String) = 0
}