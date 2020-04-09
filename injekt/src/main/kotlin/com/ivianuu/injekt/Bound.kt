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
 * Ensures instances will be resolved in the [Component] the [Binding] was added to
 */
@BehaviorMarker
val Bound = interceptingBehavior {
    it.copy(provider = BoundProvider(it.provider))
}

private class BoundProvider<T>(delegate: BindingProvider<T>) :
    DelegatingBindingProvider<T>(delegate) {

    private lateinit var boundComponent: Component

    override fun onAttach(component: Component) {
        findComponentIfNeeded(component)
        super.onAttach(boundComponent)
    }

    override fun invoke(component: Component, parameters: Parameters): T =
        super.invoke(boundComponent, parameters)

    private fun findComponentIfNeeded(component: Component) {
        if (!this::boundComponent.isInitialized) {
            this.boundComponent = component
        }
    }
}
