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
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.*

inline fun <reified T : Application> ApplicationComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ApplicationComponent(instance = instance, key = keyOf(), block = block)

inline fun <T : Application> ApplicationComponent(
    instance: T,
    key: Key<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(ApplicationScope)
        applicationBindings(instance, key)
        block()
    }

fun <T : Application> ComponentBuilder.applicationBindings(
    instance: T,
    key: Key<T>
) {
    instance(instance, key = key)
    alias(key, keyOf<Application>())
    contextBindings(ForApplication) { instance }
    maybeLifecycleBindings(
        ProcessLifecycleOwner.get(),
        ForApplication
    )
    componentAlias(ForApplication)
}

