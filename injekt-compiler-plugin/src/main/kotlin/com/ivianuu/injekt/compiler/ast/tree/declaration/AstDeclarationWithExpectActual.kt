package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual

interface AstDeclarationWithExpectActual : AstDeclaration {
    var expectActual: AstExpectActual?
}
