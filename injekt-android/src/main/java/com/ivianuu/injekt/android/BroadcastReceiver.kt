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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.StringQualifier
import com.ivianuu.injekt.StringScope
import com.ivianuu.injekt.common.addConstant
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopes

/**
 * Receiver scope
 */
object ReceiverScope : StringScope("ReceiverScope")

/**
 * Receiver qualifier
 */
object ForReceiver : StringQualifier("ForReceiver")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : BroadcastReceiver> T.receiverComponent(
    context: Context,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopes(ReceiverScope)
    getApplicationComponentOrNull(context)?.let(this::dependencies)
    addConstant(this@receiverComponent)
    definition.invoke(this)
}

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