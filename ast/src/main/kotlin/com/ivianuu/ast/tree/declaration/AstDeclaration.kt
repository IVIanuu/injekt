package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.AstExpectActual
import com.ivianuu.ast.tree.AstModality
import com.ivianuu.ast.tree.AstVisibility
import com.ivianuu.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.ast.tree.expression.AstStatement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface AstDeclaration : AstStatement {
    var parent: AstDeclarationParent
}

interface AstDeclarationContainer : AstDeclarationParent {
    val declarations: MutableList<AstDeclaration>
}

fun AstDeclarationContainer.addChild(declaration: AstDeclaration) {
    declarations += declaration
    declaration.parent = this
}

interface AstDeclarationParent : AstElement

interface AstDeclarationWithExpectActual : AstDeclaration {
    var expectActual: AstExpectActual?
}

interface AstDeclarationWithModality : AstDeclaration {
    var modality: AstModality
}

interface AstDeclarationWithName : AstDeclaration {
    var name: Name
}

interface AstDeclarationWithVisibility : AstDeclaration {
    var visibility: AstVisibility
}

interface AstOverridableDeclaration<T> : AstDeclaration {
    val overriddenDeclarations: MutableList<T>
}

abstract class AstDeclarationBase : AstDeclaration {

    override lateinit var parent: AstDeclarationParent
    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

}

val AstDeclarationWithName.fqName: FqName
    get() = when (val parent = parent) {
        is AstDeclarationWithName -> parent.fqName.child(name)
        is AstPackageFragment -> parent.packageFqName.child(name)
        else -> error("Couldn't get fq name for $this")
    }