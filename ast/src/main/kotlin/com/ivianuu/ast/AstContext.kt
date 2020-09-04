package com.ivianuu.ast

import com.ivianuu.ast.builder.AstBuilder

interface AstContext : AstBuilder {
    override val context: AstContext
        get() = this
    val builtIns: AstBuiltIns
}
