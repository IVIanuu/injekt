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
object WeakBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        WeakProvider(provider)
}

inline fun <reified T> ComponentBuilder.weak(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
): BindingContext<T> = weak(
    key = keyOf(qualifier = qualifier),
    behavior = behavior,
    duplicateStrategy = duplicateStrategy,
    provider = provider
)

/**
 * Dsl builder for [WeakBehavior] + [BoundBehavior]
 */
fun <T> ComponentBuilder.weak(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
): BindingContext<T> = bind(
    Binding(
        key = key,
        behavior = WeakBehavior + BoundBehavior() + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
)

/**
 * Annotation for the [WeakBehavior]
 */
@BehaviorMarker(WeakBehavior::class)
@Target(AnnotationTarget.CLASS)
annotation class Weak

private class WeakProvider<T>(private val provider: BindingProvider<T>) :
        (Component, Parameters) -> T, ComponentInitObserver {

    private var ref: WeakReference<Wrapper<T>>? = null

    override fun onInit(component: Component) {
        (provider as? ComponentInitObserver)?.onInit(component)
    }

    override fun invoke(p1: Component, p2: Parameters): T {
        var valueWrapper = ref?.get()
        if (valueWrapper == null) {
            valueWrapper = Wrapper(provider(p1, p2))
            ref = WeakReference(valueWrapper)
        }

        return valueWrapper.value
    }

    /**
     * We need the wrapper because [T] might be nullable
     */
    private class Wrapper<T>(val value: T)
}
