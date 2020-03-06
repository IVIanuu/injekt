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
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.keyOf

interface Container {
    val bindings: BindingMap
}

fun Container(bindings: BindingMap): Container {
    return ContainerImpl(bindings)
}

fun Container(block: ContainerBuilder.() -> Unit): Container =
    ContainerBuilder().apply(block).build()

class ContainerBuilder {

    private val bindings = mutableMapOf<Key, Binding<*>>()

    fun add(binding: Binding<*>) {
        bindings[binding.key] = binding
    }

    fun build(): Container =
        Container(
            BindingMap(
                bindings
            )
        )

}

internal class ContainerImpl(override val bindings: BindingMap) : Container {
    init {
        bindings.entries
            .map { it.value.provider }
            .filterIsInstance<ContainerLifecycleObserver<*>>()
            .forEach { it.onInit(this) }
    }
}

interface ContainerLifecycleObserver<T> {
    fun onInit(container: Container) {
    }
}

operator fun Container.plus(other: Container): Container =
    Container(
        BindingMap(
            bindings,
            other.bindings
        )
    )

inline fun <reified T> Container.get(
    name: Any? = null,
    parameters: Parameters = emptyParameters()
): T = bindings.findBinding<T>(keyOf<T>(name))!!.provider(this, parameters)
