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

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.*
import com.ivianuu.injekt.constant.constant

/**
 * View name
 */
object ForView

/**
 * Child view name
 */
object ForChildView

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : View> T.viewComponent(
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    modules, dependencies,
    { viewModule() },
    { getClosestComponentOrNull() }
)

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : View> T.childViewComponent(
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    modules, dependencies,
    { childViewModule() },
    { getClosestComponentOrNull() }
)

/**
 * Returns the closest [Component] or null
 */
fun View.getClosestComponentOrNull(): Component? {
    return getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull()
}

/**
 * Returns the closest [Component]
 */
fun View.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

/**
 * Returns the [Component] of the parent view or null
 */
fun View.getParentViewComponentOrNull(): Component? =
    (parent as? InjektTrait)?.component

/**
 * Returns the [Component] of the parent view or throws
 */
fun View.getParentViewComponent(): Component =
    getParentViewComponentOrNull() ?: error("No parent view component found for $this")

/**
 * Returns the [Component] of the context or null
 */
fun View.getContextComponentOrNull(): Component? {
    var parentContext = context
    while (parentContext != null) {
        if (parentContext is InjektTrait) {
            return parentContext.component
        }
        parentContext = (parentContext as? ContextWrapper)?.baseContext
    }

    return null
}

/**
 * Returns the [Component] of the context or throws
 */
fun View.getContextComponent(): Component =
    getContextComponentOrNull() ?: error("No context component found for $this")

/**
 * Returns the [Component] of the activity or null
 */
fun View.getApplicationComponentOrNull(): Component? =
    (context.applicationContext as? InjektTrait)?.component

/**
 * Returns the [Component] of the activity or throws
 */
fun View.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : View> T.viewModule(): Module = module {
    constant(this@viewModule) apply {
        bindType<View>()
        bindAlias<View>(ForView)
    }

    factory<Context> { context } bindName ForView
    factory { resources } bindName ForView
}

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : View> T.childViewModule(): Module = module {
    constant(this@childViewModule) apply {
        bindType<View>()
        bindAlias<View>(ForChildView)
    }

    factory<Context> { context } bindName ForChildView
    factory { resources } bindName ForChildView
}