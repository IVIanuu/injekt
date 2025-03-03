/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*

@Provide val global = ""

@Contextual fun load() {
  @Provide val logger = Logger()
  log()
}

class Logger
@Contextual fun log(sourceKey: SourceKey = inject) {
  inject<Logger>()
}
