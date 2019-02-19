package com.ivianuu.injekt.android

import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.common.addInstance

import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopeNames

const val VIEW_SCOPE = "view_scope"
const val CHILD_VIEW_SCOPE = "child_view_scope"

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <T : View> T.viewComponent(
    name: String? = javaClass.simpleName + "Component",
    deferCreateEagerInstances: Boolean = false,
    definition: ComponentDefinition = {}
): Component = component(name, deferCreateEagerInstances) {
    scopeNames(VIEW_SCOPE)
    (getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull())?.let { dependencies(it) }
    addInstance(this@viewComponent)
    definition.invoke(this)
}

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <T : View> T.childViewComponent(
    name: String? = javaClass.simpleName + "Component",
    definition: ComponentDefinition = {}
): Component = component(name) {
    scopeNames(CHILD_VIEW_SCOPE)
    (getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull())?.let { dependencies(it) }
    addInstance(this@childViewComponent)
    definition.invoke(this)
}

/**
 * Returns the [Component] of the parent view or null
 */
fun View.getParentViewComponentOrNull(): Component? =
    (parent as? InjektTrait)?.component

/**
 * Returns the [Component] of the parent view or throws
 */
fun View.getParentViewComponent(): Component =
    getParentViewComponentOrNull() ?: kotlin.error("No parent view component found for $this")

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
    getContextComponentOrNull() ?: kotlin.error("No context component found for $this")

/**
 * Returns the [Component] of the activity or null
 */
fun View.getApplicationComponentOrNull(): Component? =
    (context.applicationContext as? InjektTrait)?.component

/**
 * Returns the [Component] of the activity or throws
 */
fun View.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: kotlin.error("No application component found for $this")