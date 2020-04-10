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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.BehaviorMarker
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Bound
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.GenerateDslBuilder
import com.ivianuu.injekt.InterceptingBehavior
import com.ivianuu.injekt.Parameters
import java.util.concurrent.ConcurrentHashMap

/**
 * Returns the same instance for the given parameters
 *
 * ´´´
 * val component = Component {
 *     multi { (path: String) -> Preferences(path = path) }
 * }
 *
 * val userPrefs1 = component.get<Preferences>(parameters = parametersOf("/data/user"))
 * val userPrefs2 = component.get<Preferences>(parameters = parametersOf("/data/user"))
 * val libraryPrefs = component.get<Api>(parameters = parametersOf("/data/library"))
 * assertSame(userPrefs1, userPrefs2) // true
 * assertSame(userPrefs1, libraryPrefs) // false
 * ´´´
 *
 */
@GenerateDslBuilder
@BehaviorMarker
val Multi = InterceptingBehavior {
    it.copy(provider = MultiProvider(it.provider))
} + Bound

private class MultiProvider<T>(
    private val wrapped: BindingProvider<T>
) : (Component, Parameters) -> T {
    private val values = ConcurrentHashMap<Int, T>()
    override fun invoke(component: Component, parameters: Parameters): T =
        values.getOrPut(parameters.hashCode()) { wrapped(component, parameters) }
}
