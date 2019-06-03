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
import com.ivianuu.injekt.constant.constant

/**
 * Receiver scope
 */
@ScopeAnnotation(ReceiverScope.Companion::class)
annotation class ReceiverScope {
    companion object : NamedScope("ReceiverScope")
}

/**
 * Receiver name
 */
@Name(ForReceiver.Companion::class)
annotation class ForReceiver {
    companion object : Qualifier
}

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : BroadcastReceiver> T.receiverComponent(
    context: Context,
    scope: Scope? = ReceiverScope,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    scope, modules, dependencies,
    { receiverModule() },
    { getClosestComponentOrNull(context) }
)

/**
 * Returns the closest [Component] or null
 */
fun BroadcastReceiver.getClosestComponentOrNull(context: Context): Component? =
    getApplicationComponentOrNull(context)

/**
 * Returns the closest [Component]
 */
fun BroadcastReceiver.getClosestComponent(context: Context): Component =
    getClosestComponentOrNull(context) ?: error("No close component found for $this")

/**
 * Returns the parent [Component] if available or null
 */
fun BroadcastReceiver.getApplicationComponentOrNull(context: Context): Component? =
    (context.applicationContext as? InjektTrait)?.component

/**
 * Returns the parent [Component] or throws
 */
fun BroadcastReceiver.getApplicationComponent(context: Context): Component =
    getApplicationComponentOrNull(context) ?: error("No application component found for $this")

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : BroadcastReceiver> T.receiverModule(): Module = module {
    constant(this@receiverModule) bindType BroadcastReceiver::class
}