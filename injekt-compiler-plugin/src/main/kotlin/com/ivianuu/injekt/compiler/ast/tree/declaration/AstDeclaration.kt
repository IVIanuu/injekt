package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import org.jetbrains.kotlin.name.FqName

interface AstDeclaration : AstElement, AstAnnotationContainer {
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

abstract class AstDeclarationBase : AstDeclaration {
    override lateinit var parent: AstDeclarationParent
    override val annotations: MutableList<AstCall> = mutableListOf()

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstDeclaration> =
        accept(transformer, data) as AstTransformResult<AstDeclaration>

}

val AstDeclarationWithName.fqName: FqName
    get() = getPackageFragment()!!.packageFqName
        .child(name)
