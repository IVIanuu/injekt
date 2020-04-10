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
    val provider = BoundProvider(binding.provider)
    onBuild { provider.boundComponent = it }
    binding.copy(provider = provider)
}

private class BoundProvider<T>(private val wrapped: BindingProvider<T>) :
        (Component, Parameters) -> T {

    lateinit var boundComponent: Component

    override fun invoke(component: Component, parameters: Parameters): T {
        if (!this::boundComponent.isInitialized) boundComponent = component
        return wrapped(boundComponent, parameters)
    }

}
