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
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf

data class GivenScopeElement<S : GivenScope>(
    val key: TypeKey<Any>,
    val factory: () -> Any
)

/**
 * Registers the declaration a element in the [GivenScope] [S]
 *
 * Example:
 * ```
 * @GivenScopeElementBinding<AppComponent>
 * @Given
 * class MyAppDeps(@Given api: Api, @Given database: Database)
 *
 * fun runApp(@Given appScope: AppGivenScope) {
 *    val deps = appComponent.element<MyAppDeps>()
 * }
 * ```
 */
@Qualifier
annotation class GivenScopeElementBinding<S : GivenScope>

@Suppress("UNCHECKED_CAST")
@Given
fun <@Given T : @GivenScopeElementBinding<U> S, @ForTypeKey S, @ForTypeKey U : GivenScope>
        givenScopeElementBindingImpl(@Given factory: () -> T): GivenScopeElement<U> =
    GivenScopeElement(typeKeyOf<S>() as TypeKey<Any>, factory as () -> Any)
