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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.android

import android.app.Service
import android.content.Context
import android.content.res.Resources
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.given

object ServiceScoped : Component.Name

private object ServiceKey : Component.Key<Service>
private object ServiceComponentFactoryKey : Component.Key<(Service) -> Component<ServiceScoped>>

@GivenSetElement fun serviceComponentFactoryKey(
    builderFactory: () -> Component.Builder<ServiceScoped> = given,
) = componentElement(ApplicationScoped, ServiceComponentFactoryKey) {
    builderFactory()
        .set(ServiceKey, it)
        .build()
}

fun Service.createServiceComponent(): Component<ServiceScoped> =
    application.applicationComponent[ServiceComponentFactoryKey](this)

typealias ServiceContext = Context

@Given inline val @Given Service.serviceContext: ServiceContext
    get() = this

typealias ServiceResources = Resources

@Given inline val @Given Service.serviceResources: ServiceResources
    get() = resources
