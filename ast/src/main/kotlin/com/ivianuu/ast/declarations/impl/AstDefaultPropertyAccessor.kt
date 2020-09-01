package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.buildDefaultSetterValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitUnitTypeRef
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(AstImplementationDetail::class)
abstract class AstDefaultPropertyAccessor(
    origin: AstDeclarationOrigin,
    propertyTypeRef: AstTypeRef,
    valueParameters: MutableList<AstValueParameter>,
    isGetter: Boolean,
    visibility: Visibility,
    symbol: AstPropertyAccessorSymbol
) : AstPropertyAccessorImpl(
    origin,
    propertyTypeRef,
    valueParameters,
    body = null,
    symbol,
    isGetter,
    AstDeclarationStatusImpl(visibility, Modality.FINAL),
    annotations = mutableListOf(),
    typeParameters = mutableListOf(),
) {

    final override var body: AstBlock?
        get() = null
        set(_) {}

    companion object {
        fun createGetterOrSetter(
            origin: AstDeclarationOrigin,
            propertyTypeRef: AstTypeRef,
            visibility: Visibility,
            isGetter: Boolean
        ): AstDefaultPropertyAccessor {
            return if (isGetter) {
                AstDefaultPropertyGetter(origin, propertyTypeRef, visibility)
            } else {
                AstDefaultPropertySetter(origin, propertyTypeRef, visibility)
            }
        }
    }
}

class AstDefaultPropertyGetter(
    origin: AstDeclarationOrigin,
    propertyTypeRef: AstTypeRef,
    visibility: Visibility,
    symbol: AstPropertyAccessorSymbol = AstPropertyAccessorSymbol()
) : AstDefaultPropertyAccessor(
    origin,
    propertyTypeRef,
    valueParameters = mutableListOf(),
    isGetter = true,
    visibility = visibility,
    symbol = symbol
)

class AstDefaultPropertySetter(
    origin: AstDeclarationOrigin,
    propertyTypeRef: AstTypeRef,
    visibility: Visibility,
    symbol: AstPropertyAccessorSymbol = AstPropertyAccessorSymbol()
) : AstDefaultPropertyAccessor(
    origin,
    AstImplicitUnitTypeRef(),
    valueParameters = mutableListOf(
        buildDefaultSetterValueParameter builder@{
            this@builder.origin = origin
            this@builder.returnTypeRef = propertyTypeRef
            this@builder.symbol = AstVariableSymbol(
                CallableId(
                    FqName.ROOT,
                    Name.special("<default-setter-parameter>")
                )
            )
        }
    ),
    isGetter = false,
    visibility,
    symbol
)
