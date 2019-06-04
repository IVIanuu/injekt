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

import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.NamedScope
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.ScopeAnnotation
import com.ivianuu.injekt.bindAlias
import com.ivianuu.injekt.bindName
import com.ivianuu.injekt.bindType
import com.ivianuu.injekt.component
import com.ivianuu.injekt.constant.constant
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.module

@ScopeAnnotation(ViewScope.Companion::class)
annotation class ViewScope {
    companion object : NamedScope("ViewScope")
}

@ScopeAnnotation(ChildViewScope.Companion::class)
annotation class ChildViewScope {
    companion object : NamedScope("ChildViewScope")
}

@Name(ForView.Companion::class)
annotation class ForView {
    companion object : Qualifier
}

@Name(ForChildView.Companion::class)
annotation class ForChildView {
    companion object : Qualifier
}

fun <T : View> T.viewComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    component {
    scope = ViewScope
    getClosestComponentOrNull()?.let { dependencies(it) }
    modules(viewModule())
        block?.invoke(this)
    }

fun <T : View> T.childViewComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    component {
    scope = ChildViewScope
    getClosestComponentOrNull()?.let { dependencies(it) }
    modules(childViewModule())
        block?.invoke(this)
}

fun View.getClosestComponentOrNull(): Component? {
    return getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull()
}

fun View.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

fun View.getParentViewComponentOrNull(): Component? =
    (parent as? InjektTrait)?.component

fun View.getParentViewComponent(): Component =
    getParentViewComponentOrNull() ?: error("No parent view component found for $this")

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

fun View.getContextComponent(): Component =
    getContextComponentOrNull() ?: error("No context component found for $this")

fun View.getApplicationComponentOrNull(): Component? =
    (context.applicationContext as? InjektTrait)?.component

fun View.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

fun <T : View> T.viewModule(): Module = module {
    include(internalViewModule(ForView))
}

fun <T : View> T.childViewModule(): Module = module {
    include(internalViewModule(ForChildView))
}

private fun <T : View> T.internalViewModule(name: Qualifier) = module {
    constant(this@internalViewModule, override = true).apply {
        bindType<View>()
        bindAlias<View>(name)
    }

    factory(override = true) { context } bindName name
    factory(override = true) { resources } bindName name
}