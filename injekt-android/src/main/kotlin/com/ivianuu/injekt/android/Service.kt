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
import com.ivianuu.injekt.ComponentOwner
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.ScopeMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
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
        instance.getClosestComponentOrNull()?.let { parents(it) }
        serviceBindings(instance, key)
        block()
    }

fun <T : Service> ComponentBuilder.serviceBindings(
    instance: T,
    key: Key<T>
) {
    instance(instance, key = key)
    alias(originalKey = key, aliasKey = keyOf<Service>())
    contextBindings(ForService) { instance }
    componentAlias(ForService)
}

@ScopeMarker
val ServiceScope = Scope()

@QualifierMarker
val ForService = Qualifier()

fun Service.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Service.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun Service.getApplicationComponentOrNull(): Component? =
    (application as? ComponentOwner)?.component

fun Service.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
