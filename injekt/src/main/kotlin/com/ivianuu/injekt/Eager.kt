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
@BehaviorMarker
val Eager = InterceptingBehavior {
    it.copy(provider = EagerProvider(it.provider))
}

/**
 * Eagerly initializes the [Binding] for [key]
 *
 * @see Eager
 */
@KeyOverload
fun <T> ComponentBuilder.eager(key: Key<T>) {
    bind(
        key = key.copy(qualifier = key.qualifier + EagerInit),
        behavior = Eager,
        duplicateStrategy = DuplicateStrategy.Drop
    ) { get(key) }
}

private val EagerInit = Qualifier()

private class EagerProvider<T>(delegate: BindingProvider<T>) :
    DelegatingBindingProvider<T>(delegate) {
    override fun onAttach(component: Component) {
        super.onAttach(component)
        invoke(component, emptyParameters())
    }
}
