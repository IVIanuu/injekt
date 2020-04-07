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

import com.jakewharton.confundus.unsafeCast

/**
 * Caches the result of the first call to the provider
 *
 * We get the same instance in the following example
 *
 * ´´´
 * val component = Component {
 *     single { Database(get()) }
 * }
 *
 * val db1 = component.get<Database>()
 * val db2 = component.get<Database>()
 * assertSame(db1, db2) // true
 * ´´´
 *
 */
@BehaviorMarker
val Single = interceptingBehavior("Single") {
    it.copy(provider = SingleProvider(it.provider))
} + Bound

/**
 * Dsl builder for [Single] behavior
 */
@KeyOverload
inline fun <T> ComponentBuilder.single(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    crossinline provider: Component.(Parameters) -> T
) {
    bind(
        key = key,
        behavior = Single + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

private class SingleProvider<T>(
    delegate: BindingProvider<T>
) : DelegatingBindingProvider<T>(delegate) {
    private var value: Any? = this

    override fun invoke(component: Component, parameters: Parameters): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    this.value = super.invoke(component, parameters)
                    value = this.value
                }
            }
        }

        return value.unsafeCast()
    }
}
