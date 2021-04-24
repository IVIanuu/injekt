package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import kotlinx.coroutines.*

@Given
actual inline val ioDispatcher: IODispatcher
    get() = Dispatchers.IO
