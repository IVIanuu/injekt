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

/**
 * Creates a instance once the component is initialized
 *
 * In the following example analytics will be instantiated directly
 * without explicitly requesting it:
 *
 * ´´´
 * val component = Component {
 *     single(behavior = Eager) { Analytics() }
 * }
 * ´´´
 *
 */
annotation class Eager {
    companion object : Behavior by (InterceptingBehavior { binding ->
        val provider =
            EagerProvider(binding.provider)
        onBuild { provider.initializeIfNeeded(it) }
        binding.copy(provider = provider)
    })
}

inline fun <reified T> ComponentBuilder.eager(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    eager(
        key = keyOf(qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

fun <T> ComponentBuilder.eager(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) {
    bind(
        key = key,
        behavior = Eager + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

inline fun <reified T> ComponentBuilder.eager(
    qualifier: Qualifier = Qualifier.None
) {
    eager(key = keyOf<T>(qualifier))
}

fun <T> ComponentBuilder.eager(key: Key<T>) {
    alias(
        originalKey = key,
        aliasKey = key.copy(qualifier = EagerDelegate),
        behavior = Eager,
        duplicateStrategy = DuplicateStrategy.Drop
    )
}

private object EagerDelegate : Qualifier.Element

private class EagerProvider<T>(
    private val wrapped: BindingProvider<T>
) : (Component, Parameters) -> T {
    private var initialized = false
    override fun invoke(component: Component, parameters: Parameters): T {
        initialized = true
        return wrapped(component, parameters)
    }

    fun initializeIfNeeded(component: Component) {
        if (!initialized) invoke(component, emptyParameters())
    }
}
