package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstElement

interface AstTypeParameterContainer : AstElement {
    val typeParameters: MutableList<AstTypeParameter>
}
