package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.builder.AstClassBuilder
import com.ivianuu.ast.declarations.impl.AstAnonymousObjectImpl
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.ClassKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousObjectBuilder(override val context: AstContext) : AstClassBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    override val superTypes: MutableList<AstType> = mutableListOf()
    override val delegateInitializers: MutableList<AstDelegateInitializer> = mutableListOf()
    override lateinit var type: AstType
    lateinit var symbol: AstAnonymousObjectSymbol

    override fun build(): AstAnonymousObject {
        return AstAnonymousObjectImpl(
            context,
            annotations,
            origin,
            declarations,
            superTypes,
            delegateInitializers,
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

    @Deprecated("Modification of 'classKind' has no impact for AstAnonymousObjectBuilder", level = DeprecationLevel.HIDDEN)
    override var classKind: ClassKind
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildAnonymousObject(init: AstAnonymousObjectBuilder.() -> Unit): AstAnonymousObject {
    return AstAnonymousObjectBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstAnonymousObject.copy(init: AstAnonymousObjectBuilder.() -> Unit = {}): AstAnonymousObject {
    val copyBuilder = AstAnonymousObjectBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.declarations.addAll(declarations)
    copyBuilder.superTypes.addAll(superTypes)
    copyBuilder.delegateInitializers.addAll(delegateInitializers)
    copyBuilder.type = type
    copyBuilder.symbol = symbol
    return copyBuilder.apply(init).build()
}
