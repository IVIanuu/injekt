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
import com.ivianuu.injekt.common.addInstance

import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopeNames

const val RECEIVER_SCOPE = "receiver_scope"

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <T : BroadcastReceiver> T.receiverComponent(
    context: Context,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopeNames(RECEIVER_SCOPE)
    getApplicationComponentOrNull(context)?.let { dependencies(it) }
    addInstance(this@receiverComponent)
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
    getApplicationComponentOrNull(context) ?: error("No application found for $this")