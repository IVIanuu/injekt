package com.ivianuu.injekt.compiler.ast.tree

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class AstCallableId(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name
)
