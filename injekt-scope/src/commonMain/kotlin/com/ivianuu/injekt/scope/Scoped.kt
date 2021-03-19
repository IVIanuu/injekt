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
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForTypeKey

/**
 * Converts a [@Scoped<S> T] to a [T] which is scoped to the lifecycle of [S]
 *
 * In the following example each request to Repo resolvers to the same instance
 * ```
 * @Scoped<AppGivenScope>
 * @Given
 * class MyRepo
 *
 * fun runApp(@Given appScope: AppGivenScope) {
 *     val repo1 = given<MyRepo>()
 *     val repo2 = given<MyRepo>()
 *     // repo === repo2
 * }
 *
 * ```
 */
@Qualifier
annotation class Scoped<S : GivenScope>

@Given
inline fun <@Given T : @Scoped<U> S, @ForTypeKey S : Any, U : GivenScope> scopedImpl(
    @Given scope: U,
    @Given factory: () -> T
): S = scope.getOrCreateScopedValue<S>(factory)
