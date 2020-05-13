package com.ivianuu.injekt.compiler.transform.factory

import org.jetbrains.kotlin.name.FqName

fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"
