package com.ivianuu.injekt.compiler.resolve

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.FqName

data class Key(val type: IrType, val qualifiers: List<FqName> = emptyList())
