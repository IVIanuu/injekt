package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.impl.AstAnonymousObjectImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import org.jetbrains.kotlin.descriptors.ClassKind
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousObjectBuilder : AstClassBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override val typeParameters: MutableList<AstTypeParameterRef> = mutableListOf()
    override lateinit var classKind: ClassKind
    override val superTypeRefs: MutableList<AstTypeRef> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    lateinit var symbol: AstAnonymousObjectSymbol

    override fun build(): AstAnonymousObject {
        return AstAnonymousObjectImpl(
            origin,
            typeParameters,
            classKind,
            superTypeRefs,
            declarations,
            annotations,
            typeRef,
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
