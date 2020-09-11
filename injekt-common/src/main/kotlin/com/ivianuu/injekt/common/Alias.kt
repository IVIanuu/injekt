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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.ContextBuilder
import com.ivianuu.injekt.DuplicatePolicy
import com.ivianuu.injekt.ForKey
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.given
import com.ivianuu.injekt.keyOf

fun <@ForKey T : S, @ForKey S> ContextBuilder.alias(
    t: Key<T> = keyOf(),
    s: Key<S> = keyOf(),
    duplicatePolicy: DuplicatePolicy = DuplicatePolicy.Fail
) {
    unscoped(key = s, duplicatePolicy = duplicatePolicy) { given(t) }
}
