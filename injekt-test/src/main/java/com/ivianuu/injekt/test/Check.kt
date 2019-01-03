/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.BeanDefinition

/**
 * Checks if all [BeanDefinition]s can be resolved
 */
fun Component.check() {
    beanRegistry.getAllDefinitions()
        .map {
            println("clone for sandbox $it")
            it.cloneForSandbox()
        }
        .onEach {
            println("save $it")
            beanRegistry.saveDefinition(it)
        }
        .forEach {
            get(it.type, it.name).also { println("got $it") }
        }
}

fun <T : Any> BeanDefinition<T>.cloneForSandbox() = copy().also {
    it.kind = kind
    it.override = true
    it.createOnStart = createOnStart
    it.attributes = attributes
    it.definition = definition
    it.instance = SandboxInstance(it)
    it.moduleContext = moduleContext
}