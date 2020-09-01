package com.ivianuu.ast.declarations

import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.Modality

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstResolvedDeclarationStatus : AstDeclarationStatus {
    override val visibility: Visibility
    override val modality: Modality?
    override val isExpect: Boolean
    override val isActual: Boolean
    override val isOverride: Boolean
    override val isOperator: Boolean
    override val isInfix: Boolean
    override val isInline: Boolean
    override val isTailRec: Boolean
    override val isExternal: Boolean
    override val isConst: Boolean
    override val isLateInit: Boolean
    override val isInner: Boolean
    override val isCompanion: Boolean
    override val isData: Boolean
    override val isSuspend: Boolean
    override val isStatic: Boolean
    override val isFromSealedClass: Boolean
    override val isFromEnumClass: Boolean
    override val isFun: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitResolvedDeclarationStatus(this, data)
}
