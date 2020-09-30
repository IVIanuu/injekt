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
import android.content.res.Resources
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.MergeFactory
import com.ivianuu.injekt.merge.rootMergeFactory

val Application.applicationComponent: ApplicationComponent
    get() = ProcessLifecycleOwner.get().lifecycle.singleton {
        rootMergeFactory<ApplicationComponentFactory>()(this)
    }

@MergeFactory
typealias ApplicationComponentFactory = (Application) -> ApplicationComponent

@Module(ApplicationComponent::class)
object ApplicationModule {

    @Given
    val Application.givenApplicationContext: ApplicationContext
        get() = this

    @Given
    val Application.applicationResources: ApplicationResources
        get() = resources
}

typealias ApplicationContext = android.content.Context

typealias ApplicationResources = Resources
