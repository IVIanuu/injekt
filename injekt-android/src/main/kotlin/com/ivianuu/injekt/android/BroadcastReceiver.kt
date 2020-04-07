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

import android.content.BroadcastReceiver
import android.content.Context
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

inline fun <reified T : BroadcastReceiver> ReceiverComponent(
    context: Context,
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ReceiverComponent(
    context = context,
    instance = instance,
    key = keyOf(),
    block = block
)

inline fun <T : BroadcastReceiver> ReceiverComponent(
    context: Context,
    instance: T,
    key: Key<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component = Component {
    scopes(ReceiverScope)
    instance.getClosestComponentOrNull(context)?.let { parents(it) }
    receiverBindings(context, instance, key)
    block()
}

fun <T : BroadcastReceiver> ComponentBuilder.receiverBindings(
    context: Context,
    instance: T,
    key: Key<T>
) {
    instance(instance, key = key)
    alias(originalKey = key, aliasKey = keyOf<BroadcastReceiver>())
    contextBindings(ForReceiver) { context }
    componentAlias(ForReceiver)
}

@ScopeMarker
val ReceiverScope = Scope()

@QualifierMarker
val ForReceiver = Qualifier()

fun BroadcastReceiver.getClosestComponentOrNull(context: Context): Component? =
    getApplicationComponentOrNull(context)

fun BroadcastReceiver.getClosestComponent(context: Context): Component =
    getClosestComponentOrNull(context) ?: error("No close Component found for $this")

fun BroadcastReceiver.getApplicationComponentOrNull(context: Context): Component? =
    (context.applicationContext as? ComponentOwner)?.component

fun BroadcastReceiver.getApplicationComponent(context: Context): Component =
    getApplicationComponentOrNull(context) ?: error("No application Component found for $this")
