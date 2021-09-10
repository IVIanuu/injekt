package com.ivianuu.injekt.scope

@InternalScopeApi expect inline fun <T> synchronized(lock: Any, block: () -> T): T
