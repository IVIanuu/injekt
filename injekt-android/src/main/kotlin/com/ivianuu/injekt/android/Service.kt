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
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.EntryPoint
import com.ivianuu.injekt.merge.MergeComponent
import com.ivianuu.injekt.merge.MergeFactory
import com.ivianuu.injekt.merge.entryPoint

fun Service.createServiceComponent(): ServiceComponent {
    return application.applicationComponent
        .entryPoint<ServiceComponentEntryPoint>()
        .serviceComponentFactory(this)
}

@MergeComponent
interface ServiceComponent

@MergeFactory(ApplicationComponent::class)
typealias ServiceComponentFactory = (Service) -> ServiceComponent

typealias ServiceContext = Context

typealias ServiceResources = Resources

@Module(ServiceComponent::class)
object ServiceModule {

    @Given
    val Service.serviceContext: ServiceContext
        get() = this

    @Given
    val Service.serviceResources: ServiceResources
        get() = resources

}

@EntryPoint(ApplicationComponent::class)
interface ServiceComponentEntryPoint {
    val serviceComponentFactory: ServiceComponentFactory
}
