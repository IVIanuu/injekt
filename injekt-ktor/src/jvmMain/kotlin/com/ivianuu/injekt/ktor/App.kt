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

package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import com.ivianuu.injekt.scope.*
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*

@Provide val Application.ambients: Ambients
  get() = attributes.getOrNull(AmbientsKey)
    ?: error("No ambients found. Did you forget to call initializeAppAmbients()?")

@Provide val Routing.ambients: Ambients
  get() = application.ambients

@Provide val ApplicationCall.ambients: Ambients
  get() = application.ambients

inline fun Application.initializeAppAmbients(
  @Inject ambientsFactory: (@Provide Application) -> AmbientsFactory<ForApp>
) {
  registerAppAmbients(ambientsFactory(this).create(ambientsOf()))
}

@PublishedApi internal fun Application.registerAppAmbients(@Inject ambients: Ambients) {
  attributes.put(AmbientsKey, ambients)
  environment.monitor.subscribe(ApplicationStopped) {
    (current<Scope>() as DisposableScope).dispose()
  }
}

val AmbientsKey = AttributeKey<Ambients>("Ambients")
