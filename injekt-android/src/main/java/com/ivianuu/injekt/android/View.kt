package com.ivianuu.injekt.android

import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.addInstance
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopeNames

const val VIEW_SCOPE = "view_scope"
const val CHILD_VIEW_SCOPE = "child_view_scope"

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : View> viewComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    definition: ComponentDefinition? = null
): Component = component(name) {
    scopeNames(VIEW_SCOPE)
    (instance.getParentViewComponentOrNull()
        ?: instance.getContextComponentOrNull()
        ?: instance.getApplicationComponentOrNull())?.let { dependencies(it) }
    addInstance(instance)
    definition?.invoke(this)
}

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : View> childViewComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    definition: ComponentDefinition? = null
): Component = component(name) {
    scopeNames(CHILD_VIEW_SCOPE)
    (instance.getParentViewComponentOrNull()
        ?: instance.getContextComponentOrNull()
        ?: instance.getApplicationComponentOrNull())?.let { dependencies(it) }
    addInstance(instance)
    definition?.invoke(this)
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