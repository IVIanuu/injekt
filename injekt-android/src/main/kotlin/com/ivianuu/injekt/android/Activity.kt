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

import android.app.Activity
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.AppComponent
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.ComponentElement

/**
 * Returns a new [ActivityComponent] which must be manually stored and disposed
 */
fun Activity.createActivityComponent(): Component<ActivityComponent> =
  appComponent.element<ActivityComponent.Factory>().create(this)

object ActivityComponent {
  @Provide @ComponentElement<AppComponent>
  data class Factory(val create: (Activity) -> Component<ActivityComponent>)
}
