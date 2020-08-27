package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstModality

interface AstDeclarationWithModality : AstDeclaration {
    var modality: AstModality
}
