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
import com.ivianuu.injekt.ambient.*

/**
 * Returns a new [Ambients] including all [ForService] services which must be manually stored and disposed
 */
fun Service.createServiceAmbients(): Ambients = ambientsFromFactoryOf<ForService, Service>(
  this,
  application.appAmbients
)

abstract class ForService private constructor()

@Provide val serviceAmbientsFactoryModule =
  AmbientsFactoryModule1<ForApp, Service, ForService>()

typealias ServiceContext = Context

@Provide inline val Service.serviceContext: ServiceContext
  get() = this

typealias ServiceResources = Resources

@Provide inline val Service.serviceResources: ServiceResources
  get() = resources
