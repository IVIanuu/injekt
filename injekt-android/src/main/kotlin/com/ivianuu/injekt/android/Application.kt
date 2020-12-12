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
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.given

@Given lateinit var applicationComponent: ApplicationComponent

typealias ApplicationComponent = Component<ApplicationComponentKey<*>>

interface ApplicationComponentKey<T> : Component.Key<T>

@GivenSet fun defaultApplicationComponentElements(): ComponentElements<ApplicationComponentKey<*>> =
    emptyMap()

object ApplicationKey : ApplicationComponentKey<Application>

fun Application.initializeApp(builder: Component.Builder<ApplicationComponentKey<*>> = given) {
    applicationComponent = builder
        .set(ApplicationKey, this)
        .build()
}

typealias ApplicationStorage = Storage

@Given fun applicationStorage(component: ApplicationComponent = given): ApplicationStorage =
    component.storage

typealias ApplicationContext = Context

@Given inline fun applicationContext(component: ApplicationComponent = given): ApplicationContext =
    component[ApplicationKey]

typealias ApplicationResources = Resources

@Given inline fun applicationResources(context: Application = given): ApplicationResources =
    context.resources

typealias ApplicationLifecycleOwner = LifecycleOwner
@Given inline fun applicationLifecycleOwner(context: Application = given): ApplicationLifecycleOwner =
    ProcessLifecycleOwner.get()
