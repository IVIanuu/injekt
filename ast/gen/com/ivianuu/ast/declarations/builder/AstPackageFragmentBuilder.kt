package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstPackageFragment
import com.ivianuu.ast.declarations.builder.AstDeclarationContainerBuilder
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstPackageFragmentBuilder : AstDeclarationContainerBuilder {
    abstract override val declarations: MutableList<AstDeclaration>

    override fun build(): AstPackageFragment
}
