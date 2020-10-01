package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding

var loggingEnabled = true

interface Logger {
    fun log(msg: String)
}

object LoggerImpl : Logger {
    override fun log(msg: String) {
        println(msg)
    }
}

@Binding
fun log(
    logger: Logger?,
    @Assisted msg: () -> String,
) {
    logger?.log(msg())
}
