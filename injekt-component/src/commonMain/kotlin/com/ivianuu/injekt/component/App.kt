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

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given

typealias AppComponent = Component

/**
 * Initializes the [AppComponent] with [elements] and [initializers]
 * And can then be accessed via [appComponent]
 */
fun App.initializeApp(
    @Given elements: (@Given AppComponent) -> Set<ComponentElement<AppComponent>>,
    @Given initializers: (@Given AppComponent) -> Set<ComponentInitializer<AppComponent>>
) {
    _appComponent = ComponentBuilder<AppComponent>(elements, initializers)
        .element { this }
        .build()
}

typealias App = Any

@Given
val @Given AppComponent.app: App
    get() = element()

private lateinit var _appComponent: AppComponent

val App.appComponent: AppComponent
    get() = _appComponent
