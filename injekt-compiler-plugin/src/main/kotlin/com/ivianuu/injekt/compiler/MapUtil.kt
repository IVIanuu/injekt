package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName

val SupportedMapKeyTypes: List<FqName> = listOf(
    KotlinBuiltIns.FQ_NAMES.kClass.toSafe(),
    KotlinBuiltIns.FQ_NAMES._int.toSafe(),
    KotlinBuiltIns.FQ_NAMES._long.toSafe(),
    KotlinBuiltIns.FQ_NAMES.string.toSafe()
)