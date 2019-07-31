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
import com.ivianuu.injekt.bindClass
import com.ivianuu.injekt.component
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.module
import com.ivianuu.injekt.scopes

@Scope
annotation class ReceiverScope

@Name(ForReceiver.Companion::class)
annotation class ForReceiver {
    companion object
}

fun <T : BroadcastReceiver> T.receiverComponent(
    context: Context,
    block: (ComponentBuilder.() -> Unit)? = null
): Component = component {
    scopes<ReceiverScope>()
    getClosestComponentOrNull(context)?.let { dependencies(it) }
    modules(receiverModule(context))
    block?.invoke(this)
}

fun BroadcastReceiver.getClosestComponentOrNull(context: Context): Component? =
    getApplicationComponentOrNull(context)

fun BroadcastReceiver.getClosestComponent(context: Context): Component =
    getClosestComponentOrNull(context) ?: error("No close component found for $this")

fun BroadcastReceiver.getApplicationComponentOrNull(context: Context): Component? =
    (context.applicationContext as? InjektTrait)?.component

fun BroadcastReceiver.getApplicationComponent(context: Context): Component =
    getApplicationComponentOrNull(context) ?: error("No application component found for $this")

fun <T : BroadcastReceiver> T.receiverModule(context: Context): Module = module {
    instance(this@receiverModule) bindClass BroadcastReceiver::class
}