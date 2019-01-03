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

package com.ivianuu.injekt.android

import android.content.BroadcastReceiver
import android.content.Context
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.instanceModule

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : BroadcastReceiver> receiverComponent(
    instance: T,
    context: Context,
    name: String? = instance.javaClass.simpleName + "Component",
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, createEagerInstances) {
    components(receiverDependencies(instance, context), dropOverrides = true)
    modules(instanceModule(instance), receiverModule(instance, context))
    definition?.invoke(this)
}

/**
 * Returns components for [instance]
 */
fun receiverDependencies(instance: BroadcastReceiver, context: Context): Set<Component> {
    val dependencies = mutableSetOf<Component>()
    (context.applicationContext as? InjektTrait)?.component?.let { dependencies.add(it) }
    return dependencies
}

const val RECEIVER = "receiver"
const val RECEIVER_CONTEXT = "receiver_context"

/**
 * Returns a [Module] with convenient declarations
 */
fun <T : BroadcastReceiver> receiverModule(
    instance: T,
    context: Context,
    name: String? = "ReceiverModule"
) = module(name) {
    // service
    factory(RECEIVER) { instance as BroadcastReceiver }
    factory(RECEIVER_CONTEXT) { context }
}

fun DefinitionContext.receiver() = get<BroadcastReceiver>(RECEIVER)

fun DefinitionContext.receiverContext() = get<Context>(RECEIVER_CONTEXT)