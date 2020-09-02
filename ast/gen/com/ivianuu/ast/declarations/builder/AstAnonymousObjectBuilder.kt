package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.builder.AstClassBuilder
import com.ivianuu.ast.declarations.impl.AstAnonymousObjectImpl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.ClassKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousObjectBuilder : AstClassBuilder, AstExpressionBuilder {
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override var classKind: ClassKind = ClassKind.CLASS
    override val superTypes: MutableList<AstType> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var symbol: AstAnonymousObjectSymbol

    override fun build(): AstAnonymousObject {
        return AstAnonymousObjectImpl(
            origin,
            classKind,
            superTypes,
            declarations,
            annotations,
            type,
            symbol,
        )
    }

    @Deprecated("Modification of 'attributes' has no impact for AstAnonymousObjectBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousObject(init: AstAnonymousObjectBuilder.() -> Unit): AstAnonymousObject {
    return AstAnonymousObjectBuilder().apply(init).build()
}
