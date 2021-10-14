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

import android.app.Application
import android.content.Context
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.AppComponent

/**
 * Returns the [AppComponent] hosted in the application
 */
@Provide val Context.appComponent: AppComponent
  get() = (applicationContext as? AppComponentOwner)?.appComponent
    ?: error("application does not implement AppComponentOwner")

/**
 * Host of the [AppComponent]
 *
 * A simple [Application] implementation might look like this:
 * ```
 * class App : Application(), AppComponentOwner {
 *  override lateinit var appComponent: AppComponent
 *
 *  override fun onCreate() {
 *    appComponent = createAppComponent()
 *    super.onCreate()
 *  }
 * }
 * ```
 */
interface AppComponentOwner {
  /**
   * The [AppComponent] which is typically created via [createAppComponent]
   */
  val appComponent: AppComponent
}

/**
 * Creates the [AppComponent] which must be manually stored
 */
inline fun Application.createAppComponent(
  @Inject componentFactory: (@Provide Application) -> AppComponent
): AppComponent = componentFactory(this)
