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
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope

@Scope
annotation class ApplicationScope {
    companion object
}

@Name
annotation class ForApplication {
    companion object
}

fun <T : Application> T.ApplicationComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    Component {
        scopes(ApplicationScope)
        modules(ApplicationModule())
        block?.invoke(this)
    }

fun <T : Application> T.ApplicationModule(): Module = Module {
    instance(this@ApplicationModule).apply {
        bindType<Application>()
        bindType<Context>()
        bindAlias<Context>(ForApplication)
    }

    factory { ProcessLifecycleOwner.get() }
        .bindName(ForApplication)

    factory { resources }.bindName(ForApplication)

    withBinding<Component>(name = ApplicationScope) {
        bindName(name = ForApplication)
    }
}
