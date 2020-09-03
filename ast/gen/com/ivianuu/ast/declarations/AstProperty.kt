package com.ivianuu.ast.declarations

import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstProperty : AstVariable<AstProperty>, AstTypeParametersOwner, AstCallableDeclaration<AstProperty>, AstMemberDeclaration() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val receiverType: AstType?
    abstract override val returnType: AstType
    abstract override val name: Name
    abstract override val initializer: AstExpression?
    abstract override val delegate: AstExpression?
    abstract override val isVar: Boolean
    abstract override val getter: AstPropertyAccessor?
    abstract override val setter: AstPropertyAccessor?
    abstract override val typeParameters: List<AstTypeParameter>
    abstract override val visibility: Visibility
    abstract override val modality: Modality
    abstract override val platformStatus: PlatformStatus
    abstract override val symbol: AstPropertySymbol
    abstract val hasBackingField: Boolean
    abstract val isLocal: Boolean
    abstract val isInline: Boolean
    abstract val isConst: Boolean
    abstract val isLateinit: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitProperty(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceInitializer(newInitializer: AstExpression?)

    abstract override fun replaceDelegate(newDelegate: AstExpression?)

    abstract override fun replaceIsVar(newIsVar: Boolean)

    abstract override fun replaceGetter(newGetter: AstPropertyAccessor?)

    abstract override fun replaceSetter(newSetter: AstPropertyAccessor?)

    abstract override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>)

    abstract override fun replaceVisibility(newVisibility: Visibility)

    abstract override fun replaceModality(newModality: Modality)

    abstract override fun replacePlatformStatus(newPlatformStatus: PlatformStatus)

    abstract fun replaceHasBackingField(newHasBackingField: Boolean)

    abstract fun replaceIsLocal(newIsLocal: Boolean)

    abstract fun replaceIsInline(newIsInline: Boolean)

    abstract fun replaceIsConst(newIsConst: Boolean)

    abstract fun replaceIsLateinit(newIsLateinit: Boolean)
}
