/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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
    @Provide inline fun <T : Any> service(context: Context, serviceClass: KClass<T>):
        @SystemService T = ContextCompat.getSystemService(context, serviceClass.java)!!
  }
}
