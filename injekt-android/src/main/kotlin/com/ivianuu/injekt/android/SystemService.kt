/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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
 * fun Notification.post(@Inject notificationManager: SystemService<NotificationManager>) { ... }
 * ```
 */
data class SystemService<T>(val value: T) {
  companion object {
    @Provide fun <T : Any> invoke(context: Context, serviceClass: KClass<T>) =
       SystemService(ContextCompat.getSystemService(context, serviceClass.java)!!)
  }
}
