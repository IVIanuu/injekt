package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.builder.AstClassBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParameterRefsOwnerBuilder
import com.ivianuu.ast.declarations.impl.AstRegularClassImpl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstRegularClassBuilder : AstClassBuilder, AstTypeParameterRefsOwnerBuilder, AstAnnotationContainerBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override val annotations: MutableList<AstCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameterRef> = mutableListOf()
    open lateinit var status: AstDeclarationStatus
    override lateinit var classKind: ClassKind
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    open lateinit var name: Name
    open lateinit var symbol: AstRegularClassSymbol
    open var companionObject: AstRegularClass? = null
    override val superTypes: MutableList<AstType> = mutableListOf()

    override fun build(): AstRegularClass {
        return AstRegularClassImpl(
            origin,
            annotations,
            typeParameters,
            status,
            classKind,
            declarations,
            name,
            symbol,
            companionObject,
            superTypes,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstRegularClassBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildRegularClass(init: AstRegularClassBuilder.() -> Unit): AstRegularClass {
    return AstRegularClassBuilder().apply(init).build()
}
