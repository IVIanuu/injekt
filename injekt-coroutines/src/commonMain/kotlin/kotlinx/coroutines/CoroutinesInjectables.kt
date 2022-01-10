/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.coroutines.*

object CoroutinesInjectables {
  @OptIn(ExperimentalStdlibApi::class)
  @Provide inline fun dispatcher(context: DefaultContext): CoroutineDispatcher =
    context[CoroutineDispatcher] ?: Dispatchers.Default
}
