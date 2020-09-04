package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.impl.AstPropertyAccessorImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyAccessorBuilder(override val context: AstContext) : AstFunctionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override lateinit var returnType: AstType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    var name: Name by lazyVar { if (isSetter) Name.special("<setter>") else Name.special("<getter>") }
    var visibility: Visibility = Visibilities.Public
    var modality: Modality = Modality.FINAL
    lateinit var symbol: AstPropertyAccessorSymbol
    var isSetter: Boolean = false

    override fun build(): AstPropertyAccessor {
        return AstPropertyAccessorImpl(
            context,
            annotations,
            origin,
            returnType,
            valueParameters,
            body,
            name,
            visibility,
            modality,
            symbol,
            isSetter,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstPropertyAccessorBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildPropertyAccessor(init: AstPropertyAccessorBuilder.() -> Unit): AstPropertyAccessor {
    return AstPropertyAccessorBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstPropertyAccessor.copy(init: AstPropertyAccessorBuilder.() -> Unit = {}): AstPropertyAccessor {
    val copyBuilder = AstPropertyAccessorBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.returnType = returnType
    copyBuilder.valueParameters.addAll(valueParameters)
    copyBuilder.body = body
    copyBuilder.name = name
    copyBuilder.visibility = visibility
    copyBuilder.modality = modality
    copyBuilder.symbol = symbol
    copyBuilder.isSetter = isSetter
    return copyBuilder.apply(init).build()
}
