/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.Provide
import kotlinx.coroutines.Dispatchers

actual object IOInjectables {
  @Provide actual inline val context: IOContext
    get() = Dispatchers.IO
}
