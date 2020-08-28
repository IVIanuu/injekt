package com.ivianuu.injekt.compiler.ast.tree.declaration

import org.jetbrains.kotlin.name.Name

interface AstDeclarationWithName : AstDeclaration {
    var name: Name
}
