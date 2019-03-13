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

import android.app.Activity
import android.content.Context
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.StringQualifier
import com.ivianuu.injekt.StringScope
import com.ivianuu.injekt.bindType
import com.ivianuu.injekt.common.instance
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.scopes

/**
 * Activity scope
 */
object ActivityScope : StringScope("ActivityScope")

/**
 * Activity qualifier
 */
object ForActivity : StringQualifier("ForActivity")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : Activity> T.activityComponent(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopes(ActivityScope)
    getApplicationComponentOrNull()?.let(this::dependencies)
    modules(activityModule())
    definition.invoke(this)
}

/**
 * Returns the application [Component] or null
 */
fun Activity.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

/**
 * Returns the parent [Component] or throws
 */
fun Activity.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

/**
 * Returns a [Module] with convenient bindings
 */
inline fun <reified T : Activity> T.activityModule(): Module = module {
    instance { this@activityModule } bindType Activity::class
    instance<Context>(ForActivity) { this@activityModule }
}