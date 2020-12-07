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
import android.content.Context
import android.content.res.Resources
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.MergeSubFactory
import com.ivianuu.injekt.merge.MergeInto
import com.ivianuu.injekt.merge.mergeComponent

fun Service.createServiceComponent(): ServiceComponent =
    application.applicationComponent
        .mergeComponent<ServiceComponentFactoryOwner>()
        .serviceComponentFactoryOwner(this)

sealed class ServiceScope

interface ServiceComponent

@Scoped(ServiceScope::class)
@MergeSubFactory
typealias ServiceComponentFactory = (Service) -> ServiceComponent

typealias ServiceContext = Context
@Binding inline val Service.serviceContext: ServiceContext
    get() = this

typealias ServiceResources = Resources
@Binding inline val Service.serviceResources: ServiceResources
    get() = resources

@MergeInto(ApplicationComponent::class)
interface ServiceComponentFactoryOwner {
    val serviceComponentFactoryOwner: ServiceComponentFactory
}
