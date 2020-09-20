package com.ivianuu.injekt.compiler

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)
