package com.ivianuu.ast.declarations

sealed class AstDeclarationOrigin {
    object Source : AstDeclarationOrigin()
    object Library : AstDeclarationOrigin()
    object Java : AstDeclarationOrigin()
    object Synthetic : AstDeclarationOrigin()
    object SamConstructor : AstDeclarationOrigin()
    object FakeOverride : AstDeclarationOrigin()
    object Enhancement : AstDeclarationOrigin()
    object ImportedFromObject : AstDeclarationOrigin()
    object IntersectionOverride : AstDeclarationOrigin()
    object Delegated : AstDeclarationOrigin()

    class Plugin(val key: AstPluginKey) : AstDeclarationOrigin()
}

abstract class AstPluginKey