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

package com.ivianuu.injekt.comparison.container.impl

import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.typeOf

fun ProviderContainer(parent: Container): Container {
    return Container {
        wrap = false

        parent.bindings.entries.values
            .forEach { binding ->
                add(
                    Binding(
                        key = keyOf(
                            type = typeOf<Any?>(
                                classifier = Provider::class,
                                arguments = arrayOf(binding.key.type)
                            ),
                            name = binding.key.name
                        ),
                        overrideStrategy = binding.overrideStrategy,
                        provider = {
                            object : Provider<Any?> {
                                override fun invoke(parameters: Parameters): Any? {
                                    return get(binding.key, parameters)
                                }
                            }
                        }
                    )
                )
            }
    }
}

fun LazyContainer(parent: Container): Container {
    return Container {
        wrap = false

        parent.bindings.entries.values
            .forEach { binding ->
                add(
                    Binding(
                        key = keyOf(
                            type = typeOf<Any?>(
                                classifier = Lazy::class,
                                arguments = arrayOf(binding.key.type)
                            ),
                            name = binding.key.name
                        ),
                        overrideStrategy = binding.overrideStrategy,
                        provider = {
                            object : Lazy<Any?> {
                                private var value: Any? = this
                                override fun invoke(parameters: Parameters): Any? {
                                    if (value === this) {
                                        value = get(binding.key, parameters)
                                    }
                                    return value
                                }
                            }
                        }
                    )
                )
            }
    }
}
