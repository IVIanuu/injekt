/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
+ *
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
 * Returns the [AppScope] hosted in this application
 */
val Application.appScope: AppScope
  get() = (this as? AppScopeOwner)?.appScope
    ?: error("application does not implement AppContainerOwner")

/**
 * Host of the [AppScope]
 */
interface AppScopeOwner {
  /**
   * The [AppScope] which are typically created via [createAppScope]
   */
  val appScope: AppScope
}

/**
 * Creates the [AppScope] which must be manually stored
 */
inline fun Application.createAppScope(
  @Inject containerFactory: (@Provide Application) -> AppScope
): AppScope = containerFactory(this)

typealias AppContext = Context

@Provide inline val Application.appContext: AppContext
  get() = this

typealias AppResources = Resources

@Provide inline val AppContext.appResources: AppResources
  get() = resources

typealias AppLifecycleOwner = LifecycleOwner

@Provide inline val appLifecycleOwner: AppLifecycleOwner
  get() = ProcessLifecycleOwner.get()
