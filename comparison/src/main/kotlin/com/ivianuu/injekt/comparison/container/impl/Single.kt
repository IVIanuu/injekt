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

package com.ivianuu.injekt.comparison.container.impl

import com.ivianuu.injekt.OverrideStrategy
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.keyOf

inline fun <reified T> ContainerBuilder.single(
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoped: Boolean = true
): Unit = single(
    name = name,
    overrideStrategy = overrideStrategy,
    bound = scoped,
    provider = providerOf<T>()
)

inline fun <reified T> ContainerBuilder.single(
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    bound: Boolean = true,
    noinline provider: Container.(Parameters) -> T
) {
    add(
        Binding(
            key = keyOf<T>(name = name),
            overrideStrategy = overrideStrategy,
            provider = if (bound) BoundProvider(SingleProvider(provider))
            else SingleProvider(provider)
        )
    )
}

class SingleProvider<T>(private val provider: Container.(Parameters) -> T): (Container, Parameters) -> T {
    private var value: Any? = provider
    override fun invoke(p1: Container, p2: Parameters): T {
        if (value === this) {
            value = provider(p1, p2)
        }
        return value as T
    }
}