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
 *
 * Analytics will be instantiated directly without explicitly requesting it
 *
 * ´´´
 * val component = Component {
 *     single(behavior = EagerBehavior) { Analytics() }
 * }
 * ´´´
 *
 */
object EagerBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        EagerProvider(provider)
}

private class EagerProvider<T>(delegate: BindingProvider<T>) :
    DelegatingBindingProvider<T>(delegate) {
    override fun onAttach(component: Component) {
        super.onAttach(component)
        invoke(component, emptyParameters())
    }
}

@TagMarker
annotation class Eager {
    companion object : Tag
}

@IntoComponent(invokeOnInit = true)
private fun ComponentBuilder.eagerBindingInterceptor() {
    bindingInterceptor { binding ->
        if (Eager in binding.tags) {
            binding.copy(behavior = EagerBehavior + binding.behavior)
        } else {
            binding
        }
    }
}

@KeyOverload
inline fun <reified T> ComponentBuilder.eager(
    qualifier: Qualifier = Qualifier.None
) {
    keyOverloadStub<T>()
}

/**
 * Eagerly initializes the [Binding] for [key]
 */
fun <T> ComponentBuilder.eager(key: Key<T>) {
    bind(
        key = key.copy(qualifier = key.qualifier + EagerQualifier),
        behavior = EagerBehavior,
        duplicateStrategy = DuplicateStrategy.Drop
    ) { get(key) }
}

@QualifierMarker
private annotation class EagerQualifier {
    companion object : Qualifier.Element
}
