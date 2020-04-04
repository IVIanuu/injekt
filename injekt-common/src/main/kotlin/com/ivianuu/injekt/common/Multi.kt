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

import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.BoundBehavior
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DelegatingBindingProvider
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyOverload
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.TagMarker
import java.util.concurrent.ConcurrentHashMap

/**
 * Returns the same instance for the given parameters
 *
 * ´´´
 * val component = Component {
 *     multi { (url: String) -> Api(url = url) }
 * }
 *
 * val googleApi1 = component.get<Api>(parameters = parametersOf("www.google.com"))
 * val googleApi2 = component.get<Api>(parameters = parametersOf("www.google.com"))
 * val yahooApi = component.get<Api>(parameters = parametersOf("www.yahoo.com"))
 * assertSame(googleApi1, googleApi2) // true
 * assertSame(googleApi1, yahooApi) // false
 * ´´´
 *
 */
object MultiBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        MultiProvider(provider)
}

/**
 * Dsl builder for the [MultiBehavior] + [BoundBehavior]
 */
@KeyOverload
inline fun <T> ComponentBuilder.multi(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    crossinline provider: Component.(Parameters) -> T
) {
    bind(
        key = key,
        behavior = BoundBehavior + MultiBehavior + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

/**
 * Annotation for the [MultiBehavior]
 */
@TagMarker
annotation class Multi {
    companion object : Tag
}

@Module(invokeOnInit = true)
private fun ComponentBuilder.multiModule() {
    bindingInterceptor { binding ->
        if (Multi in binding.tags) {
            binding.copy(behavior = MultiBehavior + binding.behavior)
        } else {
            binding
        }
    }
}

private class MultiProvider<T>(
    delegate: BindingProvider<T>
) : DelegatingBindingProvider<T>(delegate) {
    private val values = ConcurrentHashMap<Int, T>()
    override fun invoke(component: Component, parameters: Parameters): T =
        values.getOrPut(parameters.hashCode()) { super.invoke(component, parameters) }
}
