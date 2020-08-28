package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstTarget
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.type.AstType

abstract class AstFunction : AstDeclarationBase(), AstAnnotationContainer,
    AstDeclarationParent,
    AstDeclarationWithVisibility,
    AstTypeParameterContainer,
    AstTarget {

    var receiver: AstType? = null

    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    val valueParameters: MutableList<AstValueParameter> = mutableListOf()

    abstract var returnType: AstType

    var body: AstBlock? = null

    override var label: String? = null

}
