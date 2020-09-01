package com.ivianuu.ast.symbols

import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.ConeClassLikeLookupTagImpl
import com.ivianuu.ast.symbols.impl.ConeClassLookupTagWithFixedSymbol
import com.ivianuu.ast.types.ConeClassLikeType
import com.ivianuu.ast.types.ConeStarProjection
import com.ivianuu.ast.types.impl.ConeClassLikeTypeImpl

fun AstClassSymbol<*>.constructStarProjectedType(typeParameterNumber: Int): ConeClassLikeType {
    return ConeClassLikeTypeImpl(
        if (classId.isLocal) ConeClassLookupTagWithFixedSymbol(classId, this)
        else ConeClassLikeLookupTagImpl(classId),
        Array(typeParameterNumber) { ConeStarProjection },
        isNullable = false
    )
}