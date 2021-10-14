package com.ivianuu.injekt.common

expect inline fun <R> synchronized(lock: Any, block: () -> R): R
