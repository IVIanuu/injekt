/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import kotlinx.coroutines.Dispatchers

actual object IOCoroutineContextModule {
  @Provide actual inline val context: IOContext
    get() = Dispatchers.Main
}
