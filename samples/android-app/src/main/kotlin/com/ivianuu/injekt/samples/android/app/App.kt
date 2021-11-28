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

package com.ivianuu.injekt.samples.android.app

import android.app.Application
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scope
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.samples.android.ui.ActivityScope
import com.ivianuu.injekt.samples.android.ui.MainActivityComponent

class App : Application() {
  @Provide private val scope = Scope<AppScope>()
  val appComponent by lazy { inject<AppComponent>() }
}

object AppScope

@Provide data class AppComponent(val mainActivityComponent: (Scope<ActivityScope>) -> MainActivityComponent)
