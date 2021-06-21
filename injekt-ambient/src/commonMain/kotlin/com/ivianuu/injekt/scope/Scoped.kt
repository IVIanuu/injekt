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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import com.ivianuu.injekt.common.*

/**
 * Reuses the same instance within scope [S]
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
@Tag annotation class Scoped<N> {
  companion object {
    @Provide inline fun <@Spread T : @Scoped<N> U, U : Any, N> scopedValue(
      factory: () -> T,
      scope: NamedScope<N>,
      key: TypeKey<U>
    ): U = scope.cache(key, factory)
  }
}
