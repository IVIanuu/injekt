/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.jvm.*

@JvmInline value class DefaultContext(val value: CoroutineContext) {
  companion object {
    @Provide inline val context: DefaultContext
      get() = DefaultContext(Dispatchers.Default)
  }
}

@JvmInline value class MainContext(val value: CoroutineContext) {
  companion object {
    @Provide inline val context: MainContext
      get() = MainContext(Dispatchers.Main)
  }
}

@JvmInline value class ImmediateMainContext(val value: CoroutineContext) {
  companion object {
    @Provide inline val context: ImmediateMainContext
      get() = ImmediateMainContext(Dispatchers.Main.immediate)
  }
}

@JvmInline value class IOContext(val value: CoroutineContext)

expect object IOInjectables {
  @Provide val context: IOContext
}
