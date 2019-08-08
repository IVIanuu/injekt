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

import android.app.Application
import android.content.Context
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.bindAlias
import com.ivianuu.injekt.bindClasses
import com.ivianuu.injekt.bindName
import com.ivianuu.injekt.component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.module
import com.ivianuu.injekt.scopes
import com.ivianuu.injekt.typeOf

@Scope
annotation class ApplicationScope

@Name(ForApplication.Companion::class)
annotation class ForApplication {
    companion object
}

fun <T : Application> T.applicationComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    component {
        scopes<ApplicationScope>()
        modules(applicationModule())
        block?.invoke(this)
    }

fun <T : Application> T.applicationModule(): Module = module {
    instance(this@applicationModule, typeOf(this@applicationModule)).apply {
        bindClasses(Application::class, Context::class)
        bindAlias<Context>(ForApplication)
    }

    factory { resources } bindName ForApplication
}