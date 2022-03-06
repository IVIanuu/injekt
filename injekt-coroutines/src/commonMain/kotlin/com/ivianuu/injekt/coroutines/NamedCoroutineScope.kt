/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.jvm.*

@JvmInline value class NamedCoroutineScope<N>(val value: CoroutineScope) : CoroutineScope, Disposable {
  override val coroutineContext: CoroutineContext
    get() = value.coroutineContext

  override fun dispose() {
    value.cancel()
  }

  companion object {
    @Provide fun <N> scope(
      context: NamedCoroutineContext<N>,
      scope: Scope<N>,
      key: TypeKey<NamedCoroutineScope<N>>
    ): NamedCoroutineScope<N> = scope(key) {
      NamedCoroutineScope(
        object : CoroutineScope, Disposable {
          override val coroutineContext: CoroutineContext = context.value + SupervisorJob()
          override fun dispose() {
            coroutineContext.cancel()
          }
        }
      )
    }
  }
}

@JvmInline value class NamedCoroutineContext<N>(val value: CoroutineContext) {
  companion object {
    @Provide fun <N> defaultContext(context: DefaultContext) = NamedCoroutineContext<N>(context.value)
  }
}
