/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
