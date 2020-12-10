package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class GivenInfo(
    val fqName: FqName,
    val key: String,
    val requiredGivens: List<Name>,
    val givensWithDefault: List<Name>,
)
