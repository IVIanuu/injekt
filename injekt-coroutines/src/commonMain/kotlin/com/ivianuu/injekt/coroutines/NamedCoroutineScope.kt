/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.jvm.*

@JvmInline value class NamedCoroutineScope<N : Scope.Name>(override val _value: Any?) : Tag<CoroutineScope> {
  companion object {
    @Provide fun <N : Scope.Name> scope(
      context: NamedCoroutineContext<N>,
      scope: Scope<N>,
      nKey: TypeKey<N>
    ): NamedCoroutineScope<N> = scope {
      NamedCoroutineScope(
        object : CoroutineScope, Disposable {
          override val coroutineContext: CoroutineContext = context() + SupervisorJob()
          override fun dispose() {
            coroutineContext.cancel()
          }
        }
      )
    }
  }
}

@JvmInline value class NamedCoroutineContext<N>(override val _value: Any?) : Tag<CoroutineContext> {
  companion object {
    @Provide inline operator fun <N> defaultContext(context: DefaultContext) =
      NamedCoroutineContext<N>(context)
  }
}
