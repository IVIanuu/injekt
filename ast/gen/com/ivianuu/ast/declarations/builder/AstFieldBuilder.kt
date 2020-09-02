package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.impl.AstFieldImpl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
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
open class AstFieldBuilder {
    open var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    open lateinit var returnType: AstType
    open lateinit var name: Name
    open lateinit var symbol: AstVariableSymbol<AstField>
    open var isVar: Boolean = false
    open val annotations: MutableList<AstFunctionCall> = mutableListOf()
    open val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    fun build(): AstField {
        return AstFieldImpl(
            origin,
            returnType,
            name,
            symbol,
            isVar,
            annotations,
            typeParameters,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildField(init: AstFieldBuilder.() -> Unit): AstField {
    return AstFieldBuilder().apply(init).build()
}
