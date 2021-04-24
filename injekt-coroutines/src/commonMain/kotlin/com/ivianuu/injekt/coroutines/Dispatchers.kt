package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import kotlinx.coroutines.*

typealias DefaultDispatcher = CoroutineDispatcher

@Given
inline val defaultDispatcher: DefaultDispatcher
    get() = Dispatchers.Default

typealias MainDispatcher = CoroutineDispatcher

@Given
inline val mainDispatcher: MainDispatcher
    get() = Dispatchers.Main

typealias ImmediateMainDispatcher = CoroutineDispatcher

@Given
inline val immediateMainDispatcher: ImmediateMainDispatcher
    get() = Dispatchers.Main.immediate

typealias IODispatcher = CoroutineDispatcher

@Given
expect val ioDispatcher: IODispatcher
