/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.Disposable
import com.ivianuu.injekt.common.Scoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

typealias NamedCoroutineScope<N> = @NamedCoroutineScopeTag<N> CoroutineScope

@Tag annotation class NamedCoroutineScopeTag<N> {
  companion object {
    @Provide fun <N> scope(
      context: NamedCoroutineContext<N>
    ): @Scoped<N> NamedCoroutineScope<N> = object : CoroutineScope, Disposable {
      override val coroutineContext: CoroutineContext = context + SupervisorJob()
      override fun dispose() {
        coroutineContext.cancel()
      }
    }
  }
}

typealias NamedCoroutineContext<N> = @NamedCoroutineContextTag<N> CoroutineContext

@Tag annotation class NamedCoroutineContextTag<N> {
  companion object {
    @Provide inline fun <N> context(context: DefaultContext): NamedCoroutineContext<N> = context
  }
}
