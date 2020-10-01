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
import android.content.res.Resources
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.merge.App
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.MergeInto

val Application.applicationComponent: ApplicationComponent
    get() = ProcessLifecycleOwner.get().lifecycle.singleton {
        Class.forName("com.ivianuu.injekt.merge.ApplicationComponentImpl")
            .constructors
            .single()
            .newInstance(this) as ApplicationComponent
    }

@MergeInto(ApplicationComponent::class)
class MergeAndroidApplicationComponent {
    @Binding
    val App.application: Application
        get() = this as Application

    @Binding
    val Application.appContext: ApplicationContext
        get() = this

    @Binding
    val Application.applicationResources: ApplicationResources
        get() = resources
}

typealias ApplicationContext = Context

typealias ApplicationResources = Resources
