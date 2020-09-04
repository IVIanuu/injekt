package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
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
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstNamedFunctionBuilder(override val context: AstContext) : AstFunctionBuilder, AstTypeParametersOwnerBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    open var dispatchReceiverType: AstType? = null
    open var extensionReceiverType: AstType? = null
    override var returnType: AstType = context.builtIns.unitType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    open var name: Name by lazyVar { symbol.callableId.callableName }
    open var visibility: Visibility = Visibilities.Public
    open var modality: Modality = Modality.FINAL
    open var platformStatus: PlatformStatus = PlatformStatus.DEFAULT
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    open var isExternal: Boolean = false
    open var isSuspend: Boolean = false
    open var isOperator: Boolean = false
    open var isInfix: Boolean = false
    open var isInline: Boolean = false
    open var isTailrec: Boolean = false
    open lateinit var symbol: AstNamedFunctionSymbol

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstNamedFunction {
        return AstNamedFunctionImpl(
            context,
            annotations,
            origin,
            dispatchReceiverType,
            extensionReceiverType,
            returnType,
            valueParameters,
            body,
            name,
            visibility,
            modality,
            platformStatus,
            typeParameters,
            isExternal,
            isSuspend,
            isOperator,
            isInfix,
            isInline,
            isTailrec,
            symbol,
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
inline fun AstBuilder.buildNamedFunction(init: AstNamedFunctionBuilder.() -> Unit): AstNamedFunction {
    return AstNamedFunctionBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstNamedFunction.copy(init: AstNamedFunctionBuilder.() -> Unit = {}): AstNamedFunction {
    val copyBuilder = AstNamedFunctionBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.dispatchReceiverType = dispatchReceiverType
    copyBuilder.extensionReceiverType = extensionReceiverType
    copyBuilder.returnType = returnType
    copyBuilder.valueParameters.addAll(valueParameters)
    copyBuilder.body = body
    copyBuilder.name = name
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.platformStatus = platformStatus
    copyBuilder.typeParameters.addAll(typeParameters)
    copyBuilder.isExternal = isExternal
    copyBuilder.isSuspend = isSuspend
    copyBuilder.isOperator = isOperator
    copyBuilder.isInfix = isInfix
    copyBuilder.isInline = isInline
    copyBuilder.isTailrec = isTailrec
    copyBuilder.symbol = symbol
    return copyBuilder.apply(init).build()
}
