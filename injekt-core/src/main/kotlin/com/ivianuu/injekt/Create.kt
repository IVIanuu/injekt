package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

fun <T> createInstance(block: @Module (() -> Unit)? = null): T = injektIntrinsic()

fun <T> createImplementation(block: @Module (() -> Unit)? = null): T = injektIntrinsic()
