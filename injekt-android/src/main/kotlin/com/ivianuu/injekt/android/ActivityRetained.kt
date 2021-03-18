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
import com.ivianuu.injekt.scope.AppGivenScope
import com.ivianuu.injekt.scope.ChildGivenScopeModule0
import com.ivianuu.injekt.scope.GivenScope
import com.ivianuu.injekt.scope.element

val ComponentActivity.activityRetainedGivenScope: ActivityRetainedGivenScope
    get() = viewModelStore.givenScope {
        application.appGivenScope.element<() -> ActivityRetainedGivenScope>()()
    }

typealias ActivityRetainedGivenScope = GivenScope

@Given
val activityRetainedGivenScopeModule =
    ChildGivenScopeModule0<AppGivenScope, ActivityRetainedGivenScope>()
