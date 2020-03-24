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

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key

internal fun Component.getAllParents(): List<Component> =
    mutableListOf<Component>().also { collectParents(it) }

private fun Component.collectParents(parents: MutableList<Component>) {
    this.parents.forEach { it.collectParents(parents) }
    parents += this.parents
}

internal data class KeyWithOverrideInfo(
    val key: Key<*>,
    val duplicateStrategy: DuplicateStrategy
)
