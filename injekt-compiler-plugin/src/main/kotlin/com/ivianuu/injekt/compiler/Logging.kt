package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given

var loggingEnabled = true

interface Logger {
    fun log(msg: String)
}

object LoggerImpl : Logger {
    override fun log(msg: String) {
        println(msg)
    }
}

@Reader
inline fun log(msg: () -> String) {
    given<Logger?>()?.log(msg())
}

@Given
fun givenLogger(): Logger? = if (loggingEnabled) LoggerImpl else null
