package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
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
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstNamedFunctionBuilder : AstFunctionBuilder, AstTypeParametersOwnerBuilder, AstAnnotationContainerBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override lateinit var returnType: AstType
    open var receiverType: AstType? = null
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    open lateinit var status: AstDeclarationStatus
    open lateinit var name: Name
    open lateinit var symbol: AstFunctionSymbol<AstNamedFunction>
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstNamedFunction {
        return AstNamedFunctionImpl(
            origin,
            returnType,
            receiverType,
            valueParameters,
            body,
            status,
            name,
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
    copyBuilder.returnType = original.returnType
    copyBuilder.receiverType = original.receiverType
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    return copyBuilder.apply(init).build()
}
