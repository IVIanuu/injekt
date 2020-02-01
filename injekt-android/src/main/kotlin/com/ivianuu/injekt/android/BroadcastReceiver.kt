/*
 * Copyright 2019 Manuel Wrage
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

import android.content.BroadcastReceiver
import android.content.Context
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : BroadcastReceiver> ReceiverComponent(
    context: Context,
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ReceiverComponent(
    context = context,
    instance = instance,
    type = typeOf(),
    block = block
)

inline fun <T : BroadcastReceiver> ReceiverComponent(
    context: Context,
    instance: T,
    type: Type<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component = Component {
    scopes(ReceiverScope)
    instance.getClosestComponentOrNull(context)?.let { dependencies(it) }
    modules(ReceiverModule(context, instance, type))
    block()
}

inline fun <reified T : BroadcastReceiver> ReceiverModule(
    context: Context,
    instance: T
): Module = ReceiverModule(context = context, instance = instance, type = typeOf())

fun <T : BroadcastReceiver> ReceiverModule(
    context: Context,
    instance: T,
    type: Type<T>
): Module = Module {
    instance(instance, type = type)
        .bindAlias<BroadcastReceiver>()

    factory(override = true) { context }
        .bindAlias(name = ForReceiver)

    withBinding<Component>(name = ReceiverScope) {
        bindAlias(name = ForReceiver)
    }
}

@Scope
annotation class ReceiverScope {
    companion object
}

@Name
annotation class ForReceiver {
    companion object
}

fun BroadcastReceiver.getClosestComponentOrNull(context: Context): Component? =
    getApplicationComponentOrNull(context)

fun BroadcastReceiver.getClosestComponent(context: Context): Component =
    getClosestComponentOrNull(context) ?: error("No close Component found for $this")

fun BroadcastReceiver.getApplicationComponentOrNull(context: Context): Component? =
    (context.applicationContext as? InjektTrait)?.component

fun BroadcastReceiver.getApplicationComponent(context: Context): Component =
    getApplicationComponentOrNull(context) ?: error("No application Component found for $this")
