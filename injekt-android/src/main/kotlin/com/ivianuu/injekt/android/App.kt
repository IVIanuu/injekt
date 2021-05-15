/*
 * Copyright 2021 Manuel Wrage
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

import android.app.*
import android.content.*
import android.content.res.*
import androidx.lifecycle.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns the [AppGivenScope] which is stored in the [Application]
 */
val Application.appGivenScope: AppGivenScope
  get() = (this as? AppGivenScopeOwner)?.appGivenScope
    ?: error("application does not implement AppGivenScopeOwner")

interface AppGivenScopeOwner {
  val appGivenScope: AppGivenScope
}

inline fun Application.createAppGivenScope(
  @Given scopeFactory: (@Given @InstallElement<AppGivenScope> Application) -> AppGivenScope
): AppGivenScope = scopeFactory(this)

typealias AppContext = Context

@Given object AppContextGivens {
  @Given inline val Application.appContext: AppContext
    get() = this
}

typealias AppResources = Resources

@Given object AppResourcesGivens {
  @Given inline val AppContext.appResources: AppResources
    get() = resources
}

typealias AppLifecycleOwner = LifecycleOwner

@Given object AppLifecycleOwnerGivens {
  @Given inline val appLifecycleOwner: AppLifecycleOwner
    get() = ProcessLifecycleOwner.get()
}
