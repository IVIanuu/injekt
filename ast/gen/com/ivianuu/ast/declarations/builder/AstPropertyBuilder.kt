package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.impl.AstPropertyImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyBuilder : AstTypeParametersOwnerBuilder, AstAnnotationContainerBuilder {
    lateinit var origin: AstDeclarationOrigin
    lateinit var returnTypeRef: AstTypeRef
    var receiverTypeRef: AstTypeRef? = null
    lateinit var name: Name
    var initializer: AstExpression? = null
    var delegate: AstExpression? = null
    var delegateFieldSymbol: AstDelegateFieldSymbol<AstProperty>? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var getter: AstPropertyAccessor? = null
    var setter: AstPropertyAccessor? = null
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var symbol: AstPropertySymbol
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var status: AstDeclarationStatus

    override fun build(): AstProperty {
        return AstPropertyImpl(
            origin,
            returnTypeRef,
            receiverTypeRef,
            name,
            initializer,
            delegate,
            delegateFieldSymbol,
            isVar,
            getter,
            setter,
            annotations,
            typeParameters,
            symbol,
            isLocal,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildProperty(init: AstPropertyBuilder.() -> Unit): AstProperty {
    return AstPropertyBuilder().apply(init).build()
}
