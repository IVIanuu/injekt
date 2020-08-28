package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstCallableId
import com.ivianuu.injekt.compiler.ast.tree.type.AstType

abstract class AstFunction : AstDeclarationBase(), AstAnnotationContainer,
    AstDeclarationParent,
    AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual
/*AstTypeParameterContainer*/ {

    abstract var callableId: AstCallableId

    //override var typeParameters: List<AstTypeParameter> = emptyList()
    val valueParameters: MutableList<AstValueParameter> = mutableListOf()

    abstract var returnType: AstType

    //var body: AstBody? = null
}

