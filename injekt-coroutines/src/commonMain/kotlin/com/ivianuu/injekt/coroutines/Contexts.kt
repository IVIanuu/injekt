/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

typealias DefaultContext = @DefaultContextTag CoroutineContext

@Tag annotation class DefaultContextTag {
  companion object {
    @Provide inline val context: DefaultContext
      get() = Dispatchers.Default
  }
}

typealias MainContext = @MainContextTag CoroutineContext

@Tag annotation class MainContextTag {
  companion object {
    @Provide inline val context: MainContext
      get() = Dispatchers.Main
  }
}

typealias ImmediateMainContext = @ImmediateMainContextTag CoroutineContext

@Tag annotation class ImmediateMainContextTag {
  companion object {
    @Provide inline val context: @ImmediateMainContextTag CoroutineContext
      get() = Dispatchers.Main.immediate
  }
}

typealias IOContext = @IOContextTag CoroutineContext

@Tag annotation class IOContextTag

expect object IOInjectables {
  @Provide val context: IOContext
}
