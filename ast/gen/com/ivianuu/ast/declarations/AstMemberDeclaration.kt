package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstFunctionCall
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstMemberDeclaration : AstPureAbstractElement(), AstNamedDeclaration {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val name: Name
    abstract val visibility: Visibility
    abstract val modality: Modality
    abstract val platformStatus: PlatformStatus

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitMemberDeclaration(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract fun replaceVisibility(newVisibility: Visibility)

    abstract fun replaceModality(newModality: Modality)

    abstract fun replacePlatformStatus(newPlatformStatus: PlatformStatus)
}
