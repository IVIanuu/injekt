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

import androidx.activity.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns the [ActivityRetainedGivenScope] of this [ComponentActivity]
 * whose lifecycle is bound the retained lifecycle of the activity
 */
val ComponentActivity.activityRetainedGivenScope: ActivityRetainedGivenScope
  get() = viewModelStore.givenScope {
    application.appGivenScope
      .element<@ChildScopeFactory () -> ActivityRetainedGivenScope>()
      .invoke()
  }

typealias ActivityRetainedGivenScope = GivenScope

@Given val activityRetainedGivenScopeModule =
  ChildScopeModule0<AppGivenScope, ActivityRetainedGivenScope>()
