/*
 * Copyright 2021 Manuel Wrage
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

import android.app.*
import android.content.*
import android.content.res.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns a new [ServiceGivenScope] which must be manually stored and disposed
 */
fun Service.createServiceGivenScope(): ServiceGivenScope =
  application.appGivenScope
    .element<@ChildScopeFactory (Service) -> ServiceGivenScope>()
    .invoke(this)

typealias ServiceGivenScope = GivenScope

@Given val serviceGivenScopeModule =
  ChildScopeModule1<AppGivenScope, Service, ServiceGivenScope>()

typealias ServiceContext = Context

@Given inline val Service.serviceContext: ServiceContext
  get() = this

typealias ServiceResources = Resources

@Given object ServiceResourcesGivens {
  @Given inline val Service.serviceResources: ServiceResources
    get() = resources
}
