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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.component
import com.ivianuu.injekt.given

@Component
interface ApplicationComponent

val Application.applicationComponent: ApplicationComponent
    get() = ProcessLifecycleOwner.get().lifecycle.singletonValue { component(this) }

typealias ApplicationContext = Context

typealias ApplicationResources = Resources

object ApplicationModule {

    @Given
    fun context(): ApplicationContext = given<Application>()

    @Given
    fun resources(): ApplicationResources = given<Application>().resources

}
