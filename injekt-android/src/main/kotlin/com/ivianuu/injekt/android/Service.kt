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

import android.app.Service
import android.content.Context
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope

@Scope
annotation class ServiceScope {
    companion object
}

@Name
annotation class ForService {
    companion object
}

fun <T : Service> T.ServiceComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    Component {
        scopes(ServiceScope)
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(ServiceModule())
        block?.invoke(this)
    }

fun Service.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Service.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun Service.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

fun Service.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")

fun <T : Service> T.ServiceModule(): Module = Module {
    instance(this@ServiceModule).apply {
        bindType<Service>()
        bindAlias<Context>(name = ForService, override = true)
        bindType<Context>(override = true)
    }

    factory(override = true) { resources }.bindName(ForService)
}
