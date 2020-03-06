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

import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyWithOverrideInfo
import com.ivianuu.injekt.OverrideStrategy
import com.ivianuu.injekt.keyOf
import java.util.UUID

fun MultiBindingContainer(parent: Container): Container {
    return Container {
        wrap = false

        val containerSets = mutableMapOf<Key, MutableSet<KeyWithOverrideInfo>>()

        parent.bindings.entries.values
            .filter { MultiBindingSets in it.properties }
            .forEach { binding ->
                val bindingSets = binding.properties.get<Set<KeyWithOverrideInfo>>(MultiBindingSets)
                bindingSets.forEach { bindingSetKey ->
                    val currentSet = containerSets.getOrPut(bindingSetKey.key) { mutableSetOf() }
                    if (currentSet.any { it.key == binding.key }) {
                        KeyWithOverrideInfo(binding.key, bindingSetKey.overrideStrategy) // todo override handling
                    }
                }
            }

        //println(containerSets)

        containerSets.forEach { (key, elementKeys) ->
            println("add set with $key to $elementKeys")
            add(
                Binding(
                    key = key,
                    overrideStrategy = OverrideStrategy.Override,
                    provider = { elementKeys.map { get<Any?>(key = it.key) }.toSet() }
                )
            )
        }
    }
}

@PublishedApi internal val MultiBindingSets = Any()

inline fun <reified T : E, reified E> ContainerBuilder.intoSet(
    setName: Any? = null,
    bindingName: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
) {
    add(
        Binding(
            key = keyOf<T>(name = UUID.randomUUID()),
            properties = propertiesOf {
                val sets = getOrPut(MultiBindingSets) { mutableSetOf<KeyWithOverrideInfo>() } as MutableSet<KeyWithOverrideInfo>
                sets += KeyWithOverrideInfo(keyOf<Set<E>>(
                    name = setName
                ), overrideStrategy = overrideStrategy)
            }
        ) { get<T>(name = bindingName, parameters = it) }
    )
}


fun main() {
    val containerA = Container {
        factory(name = "a") { "A" }
        intoSet<String, CharSequence>(bindingName = "a")
    }

    val containerB = Container {
        factory(name = "b") { "B" }
        intoSet<String, CharSequence>(bindingName = "b")
    }

    val containerC = containerA + containerB

    println(containerC.get<Set<CharSequence>>())
}