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

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.Macro
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Unqualified
import com.ivianuu.injekt.common.ForKey
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get
import com.ivianuu.injekt.component.scope

typealias ActivityRetainedComponent = Component

@Qualifier annotation class ActivityRetainedScoped
@Macro @Given inline fun <@ForKey T : @ActivityRetainedScoped S, S : Any> activityRetainedScoped(
    @Given component: ActivityRetainedComponent,
    @Given factory: () -> T
): S = component.scope(factory)

@Given val @Given ComponentActivity.activityRetainedComponent: ActivityRetainedComponent
    get() = viewModelStore.component {
        application.appComponent.get<() -> ActivityRetainedComponent>()()
    }

@GivenSetElement fun activityRetainedComponentFactory(
    @Given parent: AppComponent,
    @Given builderFactory: () -> Component.Builder<ActivityRetainedComponent>,
) = componentElement<AppComponent, () -> ActivityRetainedComponent> {
    builderFactory().dependency(parent).build()
}

@Given val @Given ActivityRetainedComponent.appComponentFromRetained: AppComponent
    get() = get()
