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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.TypeKey

/**
 * Reuses the same instance within scope [Scope] [S]
 *
 * In the following example each request to Repo resolvers to the same instance
 * ```
 * @Scoped<AppScope>
 * @Provide
 * class MyRepo
 *
 * fun runApp(@Inject appScope: AppScope) {
 *   val repo1 = inject<MyRepo>()
 *   val repo2 = inject<MyRepo>()
 *   // repo === repo2
 * }
 * ```
 */
@Tag annotation class Scoped<S : Scope> {
  companion object {
    @Provide inline fun <@Spread T : @Scoped<S> U, U : Any, S : Scope> scopedValue(
      factory: () -> T,
      scope: S,
      key: TypeKey<U>
    ): U = scoped(key = key, computation = factory)
  }
}
