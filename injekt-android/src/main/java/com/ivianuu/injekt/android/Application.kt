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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.DefinitionContext
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.addInstance
import com.ivianuu.injekt.common.bindType
import com.ivianuu.injekt.component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.scopeNames

const val APPLICATION_SCOPE = "application_scope"

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Application> T.applicationComponent(
    name: String? = javaClass.simpleName + "Component",
    deferCreateEagerInstances: Boolean = false,
    definition: ComponentDefinition? = null
): Component = component(name, deferCreateEagerInstances) {
    scopeNames(APPLICATION_SCOPE)
    addInstance(this@applicationComponent)
    modules(applicationModule())
    definition?.invoke(this)
}

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : Application> T.applicationModule(
    name: String? = "ApplicationModule"
): Module = module(name) {
    factory { this@applicationModule as Application } bindType Context::class
}

fun DefinitionContext.application(): Application = get()

fun DefinitionContext.context(): Context = get()