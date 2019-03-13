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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.Kind
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.componentName
import com.ivianuu.injekt.logger
import java.lang.ref.WeakReference

/**
 * Reusable kind
 */
object ReusableKind : Kind {

    private const val REUSABLE_KIND = "Reusable"

    override fun <T> createInstance(binding: Binding<T>, component: Component?): Instance<T> =
        ReusableInstance(binding, component)

    override fun asString(): String = REUSABLE_KIND

}

/**
 * Instance which holds a instance via [WeakReference]s
 */
class ReusableInstance<T>(
    override val binding: Binding<T>,
    val component: Component?
) : Instance<T> {

    private var _value: WeakReference<T>? = null

    override fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        val component = this.component ?: component
        val value = _value?.get()

        return if (value != null) {
            InjektPlugins.logger?.info("${component.componentName()} Return existing instance $binding")
            value
        } else {
            InjektPlugins.logger?.info("${component.componentName()} Create instance $binding")
            create(component, parameters).also { _value = WeakReference(it) }
        }
    }

}

/**
 * Provides a reusable dependency which will use weak references internally
 */
inline fun <reified T> Module.reusable(
    qualifier: Qualifier? = null,
    scope: Scope? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding(
        qualifier = qualifier,
        kind = ReusableKind,
        scope = scope,
        override = override,
        definition = definition
    )
)