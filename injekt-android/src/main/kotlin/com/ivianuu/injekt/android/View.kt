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

import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.ComponentOwner
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyOverload
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.ScopeMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

@KeyOverload
inline fun <T : View> ViewComponent(
    instance: T,
    key: Key<T>,
    scope: Scope = ViewScope,
    bindingQualifier: Qualifier = ForView,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(scope)
        instance.getClosestComponentOrNull()?.let { parents(it) }
        viewBindings(instance, key, bindingQualifier)
        block()
    }

@KeyOverload
fun <T : View> ComponentBuilder.viewBindings(
    instance: T,
    key: Key<T>,
    bindingQualifier: Qualifier = ForView
) {
    instance(
        instance = instance,
        key = key,
        duplicateStrategy = DuplicateStrategy.Override
    )
    alias(originalKey = key, aliasKey = keyOf<View>())
    alias<View>(aliasQualifier = bindingQualifier)

    contextBindings(bindingQualifier) { instance.context!! }
    componentAlias(bindingQualifier)
}

@ScopeMarker
val ViewScope = Scope()

@ScopeMarker
val ChildViewScope = Scope()

@QualifierMarker
val ForView = Qualifier()

@QualifierMarker
val ForChildView = Qualifier()

fun View.getClosestComponentOrNull(): Component? {
    return getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull()
}

fun View.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun View.getParentViewComponentOrNull(): Component? =
    (parent as? ComponentOwner)?.component

fun View.getParentViewComponent(): Component =
    getParentViewComponentOrNull() ?: error("No parent view Component found for $this")

fun View.getContextComponentOrNull(): Component? {
    var parentContext = context
    while (parentContext != null) {
        if (parentContext is ComponentOwner) {
            return parentContext.component
        }
        parentContext = (parentContext as? ContextWrapper)?.baseContext
    }

    return null
}

fun View.getContextComponent(): Component =
    getContextComponentOrNull() ?: error("No context Component found for $this")

fun View.getApplicationComponentOrNull(): Component? =
    (context.applicationContext as? ComponentOwner)?.component

fun View.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
