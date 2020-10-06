package com.ivianuu.injekt.compiler.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

data class Index(val fqName: FqName, val type: String)

data class InternalIndex(
    val index: Index,
    val originatingFile: KtFile
)