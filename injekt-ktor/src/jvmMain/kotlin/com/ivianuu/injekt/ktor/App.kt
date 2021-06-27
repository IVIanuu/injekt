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
import com.ivianuu.injekt.container.*
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*

@Provide val Application.appContainer: Container<AppScope>
  get() = attributes.getOrNull(AppContainerKey)
    ?: error("No app container installed. Did you forget to call initializeAppContainer()?")

@Provide val Routing.appContainer: Container<AppScope>
  get() = application.appContainer

@Provide val ApplicationCall.appContainer: Container<AppScope>
  get() = application.appContainer

fun Application.initializeAppContainer(
  @Inject containerFactory: (@Provide Application) -> Container<AppScope>
) {
  val container = containerFactory(this)
  attributes.put(AppContainerKey, container)
  environment.monitor.subscribe(ApplicationStopped) { container.dispose() }
}

val AppContainerKey = AttributeKey<Container<AppScope>>("AppContainer")
