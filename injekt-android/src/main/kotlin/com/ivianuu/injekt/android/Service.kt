package com.ivianuu.injekt.android

import android.app.Service
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.CompositionFactory
import com.ivianuu.injekt.EntryPoint
import com.ivianuu.injekt.InstallIn
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.createImpl
import com.ivianuu.injekt.entryPointOf
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.parent
import com.ivianuu.injekt.scope

@Scope
annotation class ServiceScoped

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class ForService

interface ServiceComponent

fun Service.serviceComponent(): Lazy<ServiceComponent> = lazy {
    entryPointOf<ServiceComponentFactoryOwner>(application.applicationComponent)
        .serviceComponentFactory(this)
}

@CompositionFactory
fun createServiceComponent(instance: Service): ServiceComponent {
    parent<ApplicationComponent>()
    scope<ServiceScoped>()
    instance(instance)
    return createImpl()
}

@InstallIn<ApplicationComponent>
@EntryPoint
interface ServiceComponentFactoryOwner {
    val serviceComponentFactory: @ChildFactory (Service) -> ServiceComponent
}
