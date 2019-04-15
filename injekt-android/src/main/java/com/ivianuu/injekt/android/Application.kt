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

import android.app.Application
import android.content.Context
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.ConstantKind
import com.ivianuu.injekt.common.constant

/**
 * Application scope
 */
object PerApplication : StringScope("PerApplication")

/**
 * Application qualifier
 */
object ForApplication : StringQualifier("ForApplication")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : Application> T.applicationComponent(
    createEagerInstances: Boolean = true,
    definition: Component.() -> Unit = {}
): Component = component(createEagerInstances) {
    scopes(PerApplication)
    modules(applicationModule())
    definition.invoke(this)
}

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : Application> T.applicationModule(): Module = module {
    add(
        Binding(
            type = this@applicationModule::class,
            kind = ConstantKind,
            definition = { this@applicationModule }
        )
    ) bindType Application::class bindType Context::class

    constant<Context>(ForApplication) { this@applicationModule }
}

fun DefinitionContext.application(): Application = get()

fun DefinitionContext.context(): Context = get()