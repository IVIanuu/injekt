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
import com.ivianuu.injekt.constant.constant

/**
 * Application scope
 */
object ApplicationScope

/**
 * Application name
 */
object ForApplication

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Application> T.applicationComponent(
    scope: Any? = ApplicationScope,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    scope, modules, dependencies,
    { applicationModule() },
    { null }
)

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : Application> T.applicationModule(): Module = module {
    constant(this@applicationModule).apply {
        bindTypes(Application::class, Context::class)
        bindAlias<Context>(ForApplication)
    }

    factory { resources } bindName ForApplication
}

fun DefinitionContext.application(): Application = get()

fun DefinitionContext.context(): Context = get()