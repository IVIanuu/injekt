package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.FunBinding

var loggingEnabled = true

interface Logger {
    fun log(msg: String)
}

object LoggerImpl : Logger {
    override fun log(msg: String) {
        println(msg)
    }
}

@FunBinding
fun log(
    logger: Logger?,
    msg: @Assisted () -> String,
) {
    logger?.log(msg())
}
