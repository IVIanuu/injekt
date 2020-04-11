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
 * Ensures instances will be resolved with the [Component] the [Binding] was added to
 */
@BehaviorMarker
val Bound = InterceptingBehavior { binding ->
    val scope = binding.behavior.foldInBehavior(null) { acc: Scope?, element ->
        acc ?: element as? Scope
    }
    val provider = BoundProvider(binding.provider, scope)
    onBuild { provider.initializeComponentIfNeeded(it) }
    binding.copy(provider = provider)
}

private class BoundProvider<T>(
    private val wrapped: BindingProvider<T>,
    private val scope: Scope?
) : (Component, Parameters) -> T {

    private lateinit var boundComponent: Component

    override fun invoke(component: Component, parameters: Parameters): T {
        initializeComponentIfNeeded(component)
        return wrapped(boundComponent, parameters)
    }

    fun initializeComponentIfNeeded(component: Component) {
        if (!this::boundComponent.isInitialized) {
            boundComponent = if (scope != null) component.getComponent(scope)
            else component
        }

    }
}
