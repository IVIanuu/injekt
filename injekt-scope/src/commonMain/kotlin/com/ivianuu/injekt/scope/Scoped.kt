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
import com.ivianuu.injekt.common.*

/**
 * Reuses the same instance within scope [S]
 *
 * In the following example each request to Repo resolvers to the same instance
 * ```
 * @Scoped<AppGivenScope>
 * @Given
 * class MyRepo
 *
 * fun runApp(@Given appScope: AppGivenScope) {
 *   val repo1 = summon<MyRepo>()
 *   val repo2 = summon<MyRepo>()
 *   // repo === repo2
 * }
 * ```
 */
@Qualifier annotation class Scoped<S : GivenScope> {
  companion object {
    @Provide inline fun <@ForEach T : @Scoped<S> U, U : Any, S : GivenScope> scopedValue(
      scope: S,
      factory: () -> T,
      key: TypeKey<U>
    ): U = scope.getOrCreateScopedValue(key, factory)
  }
}
