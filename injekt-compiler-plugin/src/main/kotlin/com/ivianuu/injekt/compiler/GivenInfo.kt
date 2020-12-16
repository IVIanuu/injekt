package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.Name

data class GivenInfo(val key: String, val givens: List<Name>) {
    companion object {
        val Empty = GivenInfo("", emptyList())
    }
}
