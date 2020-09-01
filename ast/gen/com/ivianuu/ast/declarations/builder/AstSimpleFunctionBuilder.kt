package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstSimpleFunction
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.impl.AstSimpleFunctionImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstSimpleFunctionBuilder : AstFunctionBuilder, AstTypeParametersOwnerBuilder,
    AstAnnotationContainerBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override lateinit var returnTypeRef: AstTypeRef
    open var receiverTypeRef: AstTypeRef? = null
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    open lateinit var status: AstDeclarationStatus
    open lateinit var name: Name
    open lateinit var symbol: AstFunctionSymbol<AstSimpleFunction>
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstSimpleFunction {
        return AstSimpleFunctionImpl(
            origin,
            returnTypeRef,
            receiverTypeRef,
            valueParameters,
            body,
            status,
            name,
            symbol,
            annotations,
            typeParameters,
        )
    }


    @Deprecated(
        "Modification of 'attributes' has no impact for AstSimpleFunctionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildSimpleFunction(init: AstSimpleFunctionBuilder.() -> Unit): AstSimpleFunction {
    return AstSimpleFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSimpleFunctionCopy(
    original: AstSimpleFunction,
    init: AstSimpleFunctionBuilder.() -> Unit
): AstSimpleFunction {
    val copyBuilder = AstSimpleFunctionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.receiverTypeRef = original.receiverTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    return copyBuilder.apply(init).build()
}
