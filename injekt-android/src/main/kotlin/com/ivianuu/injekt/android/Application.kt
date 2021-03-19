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
import com.ivianuu.injekt.scope.AppGivenScope
import com.ivianuu.injekt.scope.GivenScopeElementBinding

val Application.appGivenScope: AppGivenScope
    get() = (this as? AppGivenScopeOwner)?.appGivenScope
        ?: error("application does not implement AppGivenScopeOwner")

interface AppGivenScopeOwner {
    val appGivenScope: AppGivenScope
}

inline fun Application.createAppGivenScope(
    @Given scopeFactory: (@Given @GivenScopeElementBinding<AppGivenScope> Application) -> AppGivenScope
): AppGivenScope = scopeFactory(this)

typealias AppContext = Context

@Given
inline val @Given Application.appContext: AppContext
    get() = this

typealias AppResources = Resources

@Given
inline val @Given AppContext.appResources: AppResources
    get() = resources

typealias AppLifecycleOwner = LifecycleOwner

@Given
inline val appLifecycleOwner: AppLifecycleOwner
    get() = ProcessLifecycleOwner.get()
