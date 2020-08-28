package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.type.AstType

abstract class AstFunction : AstDeclarationBase(), AstAnnotationContainer,
    AstDeclarationParent,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual,
    AstTypeParameterContainer {

    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    val valueParameters: MutableList<AstValueParameter> = mutableListOf()

    abstract var returnType: AstType

    //var body: AstBody? = null
}
