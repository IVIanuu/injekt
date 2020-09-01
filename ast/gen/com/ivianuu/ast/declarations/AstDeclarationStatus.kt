package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.Visibility
import org.jetbrains.kotlin.descriptors.Modality
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDeclarationStatus : AstPureAbstractElement(), AstElement {
    abstract val visibility: Visibility
    abstract val modality: Modality
    abstract val isExpect: Boolean
    abstract val isActual: Boolean
    abstract val isOverride: Boolean
    abstract val isOperator: Boolean
    abstract val isInfix: Boolean
    abstract val isInline: Boolean
    abstract val isTailRec: Boolean
    abstract val isExternal: Boolean
    abstract val isConst: Boolean
    abstract val isLateInit: Boolean
    abstract val isInner: Boolean
    abstract val isCompanion: Boolean
    abstract val isData: Boolean
    abstract val isSuspend: Boolean
    abstract val isStatic: Boolean
    abstract val isFromSealedClass: Boolean
    abstract val isFromEnumClass: Boolean
    abstract val isFun: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDeclarationStatus(this, data)
}
