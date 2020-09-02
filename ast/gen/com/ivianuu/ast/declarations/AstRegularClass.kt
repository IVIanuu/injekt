package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstRegularClass : AstPureAbstractElement(), AstMemberDeclaration, AstTypeParametersOwner, AstClass<AstRegularClass> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeParameters: List<AstTypeParameter>
    abstract override val classKind: ClassKind
    abstract override val declarations: List<AstDeclaration>
    abstract val name: Name
    abstract val visibility: Visibility
    abstract val isExpect: Boolean
    abstract val isActual: Boolean
    abstract val modality: Modality
    abstract override val symbol: AstRegularClassSymbol
    abstract override val superTypes: List<AstType>
    abstract val isInline: Boolean
    abstract val isCompanion: Boolean
    abstract val isFun: Boolean
    abstract val isData: Boolean
    abstract val isInner: Boolean
    abstract val isExternal: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitRegularClass(this, data)
}
