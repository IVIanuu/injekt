package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.impl.AstFieldImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstFieldBuilder : AstAnnotationContainerBuilder {
    open lateinit var origin: AstDeclarationOrigin
    open lateinit var returnTypeRef: AstTypeRef
    open lateinit var name: Name
    open lateinit var symbol: AstVariableSymbol<AstField>
    open var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    open val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    open lateinit var status: AstDeclarationStatus

    override fun build(): AstField {
        return AstFieldImpl(
            origin,
            returnTypeRef,
            name,
            symbol,
            isVar,
            annotations,
            typeParameters,
            status,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildField(init: AstFieldBuilder.() -> Unit): AstField {
    return AstFieldBuilder().apply(init).build()
}
