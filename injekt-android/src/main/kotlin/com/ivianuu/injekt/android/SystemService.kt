/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android

import android.content.Context
import androidx.core.content.ContextCompat
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import kotlin.reflect.KClass

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
