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

interface ScopeOwner<in T, out S : GivenScope> {
    fun scope(value: T): S
}

fun <T, S : GivenScope> T.scope(@Given owner: ScopeOwner<T, S>): S = owner.scope(this)

fun <T, E> T.element(key: TypeKey<E>, @Given owner: ScopeOwner<T, GivenScope>): E =
    scope(owner).element(key)
