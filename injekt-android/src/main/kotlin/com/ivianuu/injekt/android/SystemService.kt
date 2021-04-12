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

import android.content.Context
import androidx.core.content.ContextCompat
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

/**
 * Allows to use any android system service
 *
 * Example:
 * ```
 * fun Notification.post(@Given notificationManager: @SystemService NotificationManager) { ... }
 * ```
 */
@Qualifier
annotation class SystemService {
    companion object {
        @Given
        fun <T : Any> systemService(
            @Given context: Context,
            @Given serviceClass: KClass<T>
        ): @SystemService T = ContextCompat.getSystemService(context, serviceClass.java)!!
    }
}
U