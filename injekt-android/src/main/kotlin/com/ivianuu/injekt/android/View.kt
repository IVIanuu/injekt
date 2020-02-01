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
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : View> ViewComponent(
    instance: T,
    scope: Any = ViewScope,
    name: Any = ForView,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    ViewComponent(instance = instance, type = typeOf(), scope = scope, name = name, block = block)

inline fun <T : View> ViewComponent(
    instance: T,
    type: Type<T>,
    scope: Any = ViewScope,
    name: Any = ForView,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(scope)
        instance.getClosestComponentOrNull()?.let { dependencies(it) }
        modules(ViewModule(instance = instance, type = type, scope = scope, name = name))
        block()
    }

inline fun <reified T : View> ViewModule(
    instance: T,
    scope: Any = ViewScope,
    name: Any = ForView
) = ViewModule(instance = instance, type = typeOf(), scope = scope, name = name)

fun <T : View> ViewModule(
    instance: T,
    type: Type<T>,
    scope: Any = ViewScope,
    name: Any = ForView
) = Module {
    instance(
        instance = instance,
        type = type,
        override = true
    ).apply {
        bindAlias<View>()
        bindAlias<View>(name)
    }

    factory(override = true) { instance.context!! }.bindAlias(name)
    factory(override = true) { instance.resources!! }.bindAlias(name)

    withBinding<Component>(name = scope) {
        bindAlias(name = name)
    }
}

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
