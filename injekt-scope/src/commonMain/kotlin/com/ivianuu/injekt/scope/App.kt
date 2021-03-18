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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Given

typealias AppGivenScope = GivenScope

/**
 * Initializes the [AppGivenScope] with [elements] and [initializers]
 * And can then be accessed via [appGivenScope]
 */
fun App.initializeApp(
    @Given elements: (@Given AppGivenScope) -> Set<GivenScopeElement<AppGivenScope>>,
    @Given initializers: (@Given AppGivenScope) -> Set<GivenScopeInitializer<AppGivenScope>>
) {
    _appGivenScope = GivenScopeBuilder<AppGivenScope>(elements, initializers)
        .element { this }
        .build()
}

typealias App = Any

@Given
val @Given AppGivenScope.app: App
    get() = element()

private var _appGivenScope: AppGivenScope? = null

val App.appGivenScope: AppGivenScope
    get() = _appGivenScope ?: error("app given scope not initialized. Did you forget to call initializeApp()?")
