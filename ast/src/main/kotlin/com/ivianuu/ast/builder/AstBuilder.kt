package com.ivianuu.ast.builder

import com.ivianuu.ast.AstContext

interface AstBuilder {
    val context: AstContext
}

fun AstBuilder(context: AstContext): AstBuilder = AstBuilderImpl(context)

private class AstBuilderImpl(override val context: AstContext) : AstBuilder
