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

import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.typeOf

@Scope
annotation class ViewScope {
    companion object
}

@Scope
annotation class ChildViewScope {
    companion object
}

@Name
annotation class ForView {
    companion object
}

@Name
annotation class ForChildView {
    companion object
}

fun <T : View> T.ViewComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    Component {
        scopes(ViewScope)
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(ViewModule())
        block?.invoke(this)
    }

fun <T : View> T.ChildViewComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    Component {
        scopes(ChildViewScope)
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(ChildViewModule())
        block?.invoke(this)
    }

fun View.getClosestComponentOrNull(): Component? {
    return getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull()
}

fun View.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun View.getParentViewComponentOrNull(): Component? =
    (parent as? InjektTrait)?.component

fun View.getParentViewComponent(): Component =
    getParentViewComponentOrNull() ?: error("No parent view Component found for $this")

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
    getContextComponentOrNull() ?: error("No context Component found for $this")

fun View.getApplicationComponentOrNull(): Component? =
    (context.applicationContext as? InjektTrait)?.component

fun View.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")

fun <T : View> T.ViewModule(): Module = Module {
    include(InternalViewModule(scope = ViewScope, name = ForView))
}

fun <T : View> T.ChildViewModule(): Module = Module {
    include(InternalViewModule(scope = ChildViewScope, name = ForChildView))
}

private fun <T : View> T.InternalViewModule(scope: Any, name: Any) = Module {
    instance(
        instance = this@InternalViewModule,
        type = typeOf(this@InternalViewModule),
        override = true
    ).apply {
        bindAlias<View>()
        bindAlias<View>(name)
    }

    factory(override = true) { context!! }.bindAlias(name)
    factory(override = true) { resources!! }.bindAlias(name)

    withBinding<Component>(name = scope) {
        bindAlias(name = name)
    }
}
