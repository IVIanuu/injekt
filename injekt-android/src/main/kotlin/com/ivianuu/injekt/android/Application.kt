package com.ivianuu.injekt.android

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.CompositionFactory
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.compositionFactoryOf
import com.ivianuu.injekt.inject

@Scope
annotation class ApplicationScoped

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class ForApplication

interface ApplicationComponent

val Application.applicationComponent: ApplicationComponent
    get() = ProcessLifecycleOwner.get().lifecycle.singleton {
        compositionFactoryOf<@CompositionFactory (Application) -> ApplicationComponent>()
            .invoke(this)
    }

/*
@CompositionFactory
fun createApplicationComponent(instance: Application): ApplicationComponent {
    scope<ApplicationScoped>()
    instance(instance)
    return createImpl()
}*/

fun <T : Application> T.inject() {
    inject(applicationComponent)
}
