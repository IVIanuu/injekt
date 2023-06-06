/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

typealias DefaultCoroutineContext = @DefaultCoroutineContextTag CoroutineContext

@Tag annotation class DefaultCoroutineContextTag {
  companion object {
    @Provide inline val context: DefaultCoroutineContext
      get() = Dispatchers.Default
  }
}

typealias MainCoroutineContext = @MainCoroutineContextTag CoroutineContext

@Tag annotation class MainCoroutineContextTag {
  companion object {
    @Provide inline val context: MainCoroutineContext
      get() = Dispatchers.Main
  }
}

typealias IOCoroutineContext = @IOCoroutineContextTag CoroutineContext

@Tag annotation class IOCoroutineContextTag

expect object IOCoroutineContextModule {
  @Provide val context: IOCoroutineContext
}
