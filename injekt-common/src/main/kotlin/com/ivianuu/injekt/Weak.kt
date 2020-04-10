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
@GenerateDslBuilder
@BehaviorMarker
val Weak = InterceptingBehavior {
    it.copy(provider = WeakProvider(it.provider))
} + Bound

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

    /**
     * We need the wrapper because [T] might be nullable
     */
    private class Wrapper<T>(val value: T)
}
