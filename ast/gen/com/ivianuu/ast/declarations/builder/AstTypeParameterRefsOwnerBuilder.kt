package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.AstTypeParameterRefsOwner

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstTypeParameterRefsOwnerBuilder {
    abstract val typeParameters: MutableList<AstTypeParameterRef>

    fun build(): AstTypeParameterRefsOwner
}
