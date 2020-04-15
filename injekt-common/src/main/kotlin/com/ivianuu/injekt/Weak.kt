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

package com.ivianuu.injekt

import java.lang.ref.WeakReference

/**
 * Holds instances in a [WeakReference]
 */
annotation class Weak {
    companion object : Behavior by (InterceptingBehavior {
        it.copy(provider = WeakProvider(it.provider))
    } + Bound)
}

inline fun <reified T> ComponentBuilder.weak(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    weak(
        key = keyOf(qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

fun <T> ComponentBuilder.weak(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) {
    bind(
        key = key,
        behavior = Weak + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

private class WeakProvider<T>(private val wrapped: BindingProvider<T>) :
        (Component, Parameters) -> T {
    private var ref: WeakReference<Wrapper<T>>? = null

    override fun invoke(component: Component, parameters: Parameters): T {
        var valueWrapper = ref?.get()
        if (valueWrapper == null) {
            valueWrapper = Wrapper(
                wrapped(
                    component,
                    parameters
                )
            )
            ref = WeakReference(valueWrapper)
        }

        return valueWrapper.value
    }

    private class Wrapper<T>(val value: T)
}
