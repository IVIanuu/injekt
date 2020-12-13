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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.android

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.given

@Given lateinit var _applicationComponent: Component<ApplicationScoped>

@Suppress("unused")
val Application.applicationComponent: Component<ApplicationScoped>
    get() = _applicationComponent

object ApplicationScoped : Component.Name

object ApplicationKey : Component.Key<Application>

fun Application.initializeApp(builder: Component.Builder<ApplicationScoped> = given) {
    _applicationComponent = builder
        .set(ApplicationKey, this)
        .build()
}

@Given inline val @Given Component<ApplicationScoped>.application: Application
    get() = this[ApplicationKey]

typealias ApplicationContext = Context

@Given inline val @Given Application.appContext: ApplicationContext
    get() = this

typealias ApplicationResources = Resources

@Given inline val @Given ApplicationContext.applicationResources: ApplicationResources
    get() = resources

typealias ApplicationLifecycleOwner = LifecycleOwner

@Suppress("unused")
@Given inline val @Given Application.applicationLifecycleOwner: ApplicationLifecycleOwner
    get() = ProcessLifecycleOwner.get()
