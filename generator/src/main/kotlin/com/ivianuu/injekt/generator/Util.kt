package com.ivianuu.injekt.generator

const val MAX_PARAMETERS = 22

fun commaSeparated(parameterCount: Int, block: (Int) -> String): String =
    (1..parameterCount).joinToString { block(it) }
