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

import android.content.*
import androidx.core.content.*
import com.ivianuu.injekt.*
import kotlin.reflect.*

/**
 * Tag for android system services
 *
 * Example:
 * ```
 * fun Notification.post(@Inject notificationManager: @SystemService NotificationManager) { ... }
 * ```
 */
@Tag annotation class SystemService {
  companion object {
    @Provide inline fun <T : Any> systemService(context: Context, serviceClass: KClass<T>):
        @SystemService T = ContextCompat.getSystemService(context, serviceClass.java)!!
  }
}
