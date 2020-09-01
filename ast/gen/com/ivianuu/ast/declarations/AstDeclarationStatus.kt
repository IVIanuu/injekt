package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.Modality

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstDeclarationStatus : AstElement {
    val visibility: Visibility
    val modality: Modality?
    val isExpect: Boolean
    val isActual: Boolean
    val isOverride: Boolean
    val isOperator: Boolean
    val isInfix: Boolean
    val isInline: Boolean
    val isTailRec: Boolean
    val isExternal: Boolean
    val isConst: Boolean
    val isLateInit: Boolean
    val isInner: Boolean
    val isCompanion: Boolean
    val isData: Boolean
    val isSuspend: Boolean
    val isStatic: Boolean
    val isFromSealedClass: Boolean
    val isFromEnumClass: Boolean
    val isFun: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDeclarationStatus(this, data)
}
