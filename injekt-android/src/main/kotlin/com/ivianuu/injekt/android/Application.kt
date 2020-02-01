/*
 * Copyright 2020 Manuel Wrage
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
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : Application> ApplicationComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ApplicationComponent(instance = instance, type = typeOf(), block = block)

inline fun <T : Application> ApplicationComponent(
    instance: T,
    type: Type<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(ApplicationScope)
        modules(ApplicationModule(instance, type))
        block()
    }

inline fun <reified T : Application> ApplicationModule(
    instance: T
): Module = ApplicationModule(instance = instance, type = typeOf())

fun <T : Application> ApplicationModule(
    instance: T,
    type: Type<T>
): Module = Module {
    instance(instance, type = type).apply {
        bindAlias<Application>()
        bindAlias<Context>()
        bindAlias<Context>(name = ForApplication)
    }

    factory { ProcessLifecycleOwner.get() }
        .bindAlias(name = ForApplication)

    factory { instance.resources!! }.bindAlias(name = ForApplication)

    withBinding<Component>(name = ApplicationScope) {
        bindAlias(name = ForApplication)
    }
}

@Scope
annotation class ApplicationScope {
    companion object
}

@Name
annotation class ForApplication {
    companion object
}
