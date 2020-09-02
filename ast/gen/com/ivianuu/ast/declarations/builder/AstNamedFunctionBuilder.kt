package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParametersOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstNamedFunctionImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstNamedFunctionBuilder : AstFunctionBuilder, AstTypeParametersOwnerBuilder {
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    open var receiverType: AstType? = null
    override lateinit var returnType: AstType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    open lateinit var name: Name
    open var visibility: Visibility = Visibilities.Public
    open var isExpect: Boolean = false
    open var isActual: Boolean = false
    open var modality: Modality = Modality.FINAL
    open var isExternal: Boolean = false
    open var isSuspend: Boolean = false
    open var isOperator: Boolean = false
    open var isInfix: Boolean = false
    open var isInline: Boolean = false
    open var isTailrec: Boolean = false
    open lateinit var symbol: AstFunctionSymbol<AstNamedFunction>
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstNamedFunction {
        return AstNamedFunctionImpl(
            origin,
            receiverType,
            returnType,
            valueParameters,
            body,
            name,
            visibility,
            isExpect,
            isActual,
            modality,
            isExternal,
            isSuspend,
            isOperator,
            isInfix,
            isInline,
            isTailrec,
            symbol,
            annotations,
            typeParameters,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstNamedFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedFunction(init: AstNamedFunctionBuilder.() -> Unit): AstNamedFunction {
    return AstNamedFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedFunctionCopy(original: AstNamedFunction, init: AstNamedFunctionBuilder.() -> Unit): AstNamedFunction {
    val copyBuilder = AstNamedFunctionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.receiverType = original.receiverType
    copyBuilder.returnType = original.returnType
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.name = original.name
    copyBuilder.visibility = original.visibility
    copyBuilder.isExpect = original.isExpect
    copyBuilder.isActual = original.isActual
    copyBuilder.modality = original.modality
    copyBuilder.isExternal = original.isExternal
    copyBuilder.isSuspend = original.isSuspend
    copyBuilder.isOperator = original.isOperator
    copyBuilder.isInfix = original.isInfix
    copyBuilder.isInline = original.isInline
    copyBuilder.isTailrec = original.isTailrec
    copyBuilder.symbol = original.symbol
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    return copyBuilder.apply(init).build()
}
