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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.ScopeMarker
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

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

        instance(instance, key = key)
            .bindAlias<Application>()
        contextBindings(ForApplication) { instance }
        maybeLifecycleBindings(
            ProcessLifecycleOwner.get(),
            ForApplication
        )
        componentAlias(ForApplication)

        block()
    }

@ScopeMarker
annotation class ApplicationScope {
    companion object : Scope
}

@QualifierMarker
annotation class ForApplication {
    companion object : Qualifier.Element
}
