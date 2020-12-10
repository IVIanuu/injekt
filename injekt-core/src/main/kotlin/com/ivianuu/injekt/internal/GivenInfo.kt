package com.ivianuu.injekt.internal

annotation class GivenInfo(
    val key: String,
    val requiredGivens: Array<String>,
    val givensWithDefault: Array<String>,
)
