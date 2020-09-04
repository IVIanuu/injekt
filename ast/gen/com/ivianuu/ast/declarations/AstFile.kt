package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstFunctionCall
import org.jetbrains.kotlin.name.FqName
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstFile : AstPureAbstractElement(), AstPackageFragment, AstAnnotationContainer {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val declarations: List<AstDeclaration>
    abstract val name: String
    abstract val packageFqName: FqName

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFile(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceDeclarations(newDeclarations: List<AstDeclaration>)

    abstract fun replaceName(newName: String)

    abstract fun replacePackageFqName(newPackageFqName: FqName)
}
