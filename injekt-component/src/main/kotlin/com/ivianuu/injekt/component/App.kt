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
import com.ivianuu.injekt.Macro
import com.ivianuu.injekt.TypeParameterFix
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForKey

typealias AppComponent = Component

@Qualifier annotation class Scoped<C : Component>

@TypeParameterFix("T", Scoped::class, ["C"])
@Macro @Given inline fun <@ForKey T : @Scoped<C> S, @ForKey S : Any, @ForKey C : Component> scopedImpl(
    @Given component: C,
    @Given factory: () -> T
): S = component.scope(factory)

fun App.initializeApp(
    @Given elementsFactory: (@Given AppComponent) -> Set<ComponentElement<AppComponent>>
) {
    _appComponent = ComponentBuilder(elementsFactory)
        .element(this)
        .build()
}

typealias App = Any

@Given val @Given AppComponent.app: App
    get() = get()

private lateinit var _appComponent: AppComponent
@Given val @Given App.appComponent: AppComponent
    get() = _appComponent
