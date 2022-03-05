/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android

import android.content.*
import androidx.core.content.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import kotlin.reflect.*

/**
 * Tag for android system services
 *
 * Example:
 * ```
 * fun Notification.post(@Inject notificationManager: SystemService<NotificationManager>) { ... }
 * ```
 */
@JvmInline value class SystemService<T : Any>(override val _value: Any) : Tag<T> {
  companion object {
    @Provide fun <T : Any> invoke(context: Context, serviceClass: KClass<T>) =
       SystemService<T>(ContextCompat.getSystemService(context, serviceClass.java)!!)
  }
}
