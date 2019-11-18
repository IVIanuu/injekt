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
import com.ivianuu.injekt.component
import com.ivianuu.injekt.module
import com.ivianuu.injekt.typeOf

@Scope
annotation class ServiceScope

@Name(ForService.Companion::class)
annotation class ForService {
    companion object
}

fun <T : Service> T.serviceComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    component {
        scopes<ServiceScope>()
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(serviceModule())
        block?.invoke(this)
    }

fun Service.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Service.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

fun Service.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

fun Service.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

fun <T : Service> T.serviceModule(): Module = module {
    instance(this@serviceModule, typeOf(this@serviceModule), override = true).apply {
        bindType<Service>()
        bindAlias<Context>(ForService)
        bindType<Context>()
    }

    factory(override = true) { resources }.bindName(ForService)
}