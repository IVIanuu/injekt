package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.Name

data class GivenInfo(
    val key: String,
    val requiredGivens: List<Name>,
    val givensWithDefault: List<Name>,
) {
    val allGivens = requiredGivens + givensWithDefault

    companion object {
        val Empty = GivenInfo("", emptyList(), emptyList())
    }
}
