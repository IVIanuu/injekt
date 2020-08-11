package com.ivianuu.injekt.internal

internal inline fun <C, R> runReaderDummy(
    context: C,
    block: (C) -> R
) = block(context)
