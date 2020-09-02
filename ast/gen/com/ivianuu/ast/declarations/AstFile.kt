package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import org.jetbrains.kotlin.name.FqName
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstFile : AstPureAbstractElement(), AstPackageFragment, AstAnnotationContainer {
    abstract override val annotations: List<AstAnnotationCall>
    abstract val declarations: List<AstDeclaration>
    abstract val name: String
    abstract override val packageFqName: FqName

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFile(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFile

    abstract fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstFile
}
