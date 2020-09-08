package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstAbstractConstructorBuilder
import com.ivianuu.ast.declarations.impl.AstConstructorImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstConstructorBuilder(override val context: AstContext) : AstAbstractConstructorBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override var attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override var dispatchReceiverType: AstType? = null
    override var extensionReceiverType: AstType? = null
    override lateinit var returnType: AstType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override lateinit var symbol: AstConstructorSymbol
    override var delegatedConstructor: AstDelegatedConstructorCall? = null
    override var body: AstBlock? = null
    override var visibility: Visibility = Visibilities.Public
    var isPrimary: Boolean = false

    override fun build(): AstConstructor {
        return AstConstructorImpl(
            context,
            annotations,
            origin,
            attributes,
            dispatchReceiverType,
            extensionReceiverType,
            returnType,
            valueParameters,
            symbol,
            delegatedConstructor,
            body,
            visibility,
            isPrimary,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildConstructor(init: AstConstructorBuilder.() -> Unit): AstConstructor {
    return AstConstructorBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstConstructor.copy(init: AstConstructorBuilder.() -> Unit = {}): AstConstructor {
    val copyBuilder = AstConstructorBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.attributes = attributes
    copyBuilder.dispatchReceiverType = dispatchReceiverType
    copyBuilder.extensionReceiverType = extensionReceiverType
    copyBuilder.returnType = returnType
    copyBuilder.valueParameters.addAll(valueParameters)
    copyBuilder.symbol = symbol
    copyBuilder.delegatedConstructor = delegatedConstructor
    copyBuilder.body = body
    copyBuilder.visibility = visibility
    copyBuilder.isPrimary = isPrimary
    return copyBuilder.apply(init).build()
}
