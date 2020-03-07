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

package com.ivianuu.injekt.android

import android.app.Service
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.keyOf

inline fun <reified T : Service> ServiceComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ServiceComponent(instance = instance, key = keyOf(), block = block)

inline fun <T : Service> ServiceComponent(
    instance: T,
    key: Key<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(ServiceScope)
        instance.getClosestComponentOrNull()?.let { dependencies(it) }

        instance(instance, key = key)
            .bindAlias<Service>()
        contextBindings(ForService) { instance }
        componentAlias(ServiceScope)

        block()
    }

@Scope
annotation class ServiceScope {
    companion object
}

@Name
annotation class ForService {
    companion object
}

fun Service.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Service.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun Service.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

fun Service.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
