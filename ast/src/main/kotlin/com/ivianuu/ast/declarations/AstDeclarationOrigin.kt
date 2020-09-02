package com.ivianuu.ast.declarations

sealed class AstDeclarationOrigin {
    object Source : AstDeclarationOrigin()
    object Library : AstDeclarationOrigin()
}
