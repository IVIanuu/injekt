package com.ivianuu.injekt.android

import android.content.ContextWrapper
import android.view.View
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.StringQualifier
import com.ivianuu.injekt.StringScope
import com.ivianuu.injekt.common.addInstance
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopes

/**
 * View scope
 */
object ViewScope : StringScope("ViewScope")

/**
 * Child view scope
 */
object ChildViewScope : StringScope("ChildViewScope")

/**
 * View qualifier
 */
object ForView : StringQualifier("ForView")

/**
 * Child view qualifier
 */
object ForChildView : StringQualifier("ForChildView")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : View> T.viewComponent(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopes(ViewScope)
    (getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull())?.let(this::dependencies)
    addInstance(this@viewComponent)
    definition.invoke(this)
}

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : View> T.childViewComponent(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopes(ChildViewScope)
    (getParentViewComponentOrNull()
        ?: getContextComponentOrNull()
        ?: getApplicationComponentOrNull())?.let(this::dependencies)
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