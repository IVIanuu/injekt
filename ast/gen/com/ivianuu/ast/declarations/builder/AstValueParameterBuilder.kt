package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.impl.AstValueParameterImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstValueParameterBuilder : AstAnnotationContainerBuilder {
    open lateinit var origin: AstDeclarationOrigin
    open lateinit var returnType: AstType
    open lateinit var name: Name
    open lateinit var symbol: AstVariableSymbol<AstValueParameter>
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    open var defaultValue: AstExpression? = null
    open var isCrossinline: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    open var isNoinline: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    open var isVararg: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstValueParameter {
        return AstValueParameterImpl(
            origin,
            returnType,
            name,
            symbol,
            annotations,
            defaultValue,
            isCrossinline,
            isNoinline,
            isVararg,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameter(init: AstValueParameterBuilder.() -> Unit): AstValueParameter {
    return AstValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameterCopy(original: AstValueParameter, init: AstValueParameterBuilder.() -> Unit): AstValueParameter {
    val copyBuilder = AstValueParameterBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.returnType = original.returnType
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.defaultValue = original.defaultValue
    copyBuilder.isCrossinline = original.isCrossinline
    copyBuilder.isNoinline = original.isNoinline
    copyBuilder.isVararg = original.isVararg
    return copyBuilder.apply(init).build()
}
