package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstVisibility

interface AstDeclarationWithVisibility : AstDeclaration {
    var visibility: AstVisibility
}
