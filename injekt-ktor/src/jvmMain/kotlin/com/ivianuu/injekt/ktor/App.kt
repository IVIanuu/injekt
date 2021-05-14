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
import com.ivianuu.injekt.scope.*
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*

val Application.appGivenScope: AppGivenScope
  get() = attributes.getOrNull(AppGivenScopeKey)
    ?: error("No app given scope found. Did you forget to call initializeAppGivenScope()?")

val Routing.appGivenScope: AppGivenScope
  get() = application.appGivenScope

val ApplicationCall.appGivenScope: AppGivenScope
  get() = application.appGivenScope

inline fun Application.initializeAppGivenScope(
  @Given scopeFactory: (@Given @InstallElement<AppGivenScope> Application) -> AppGivenScope
) {
  val scope = scopeFactory(this)
  registerAppGivenScope(scope)
}

@PublishedApi
internal fun Application.registerAppGivenScope(scope: AppGivenScope) {
  attributes.put(AppGivenScopeKey, scope)
  environment.monitor.subscribe(ApplicationStopped) {
    scope.dispose()
  }
}

private val AppGivenScopeKey = AttributeKey<AppGivenScope>("AppGivenScope")
