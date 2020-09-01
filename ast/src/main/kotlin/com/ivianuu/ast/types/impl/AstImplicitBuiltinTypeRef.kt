package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstFakeSourceElementKind
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.fakeElement
import com.ivianuu.ast.symbols.StandardClassIds
import com.ivianuu.ast.symbols.impl.ConeClassLikeLookupTagImpl
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.ConeClassLikeType
import com.ivianuu.ast.types.ConeTypeProjection
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.ClassId

sealed class AstImplicitBuiltinTypeRef(
    val id: ClassId,
    typeArguments: Array<out ConeTypeProjection> = emptyArray(),
    isNullable: Boolean = false
) : AstResolvedTypeRef() {
    override val annotations: List<AstAnnotationCall>
        get() = emptyList()

    override val type: ConeClassLikeType =
        ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(id), typeArguments, isNullable)

    override val delegatedTypeRef: AstTypeRef?
        get() = null

    override val isSuspend: Boolean
        get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedTypeRef {
        return this
    }
}

class AstImplicitUnitTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Unit)

class AstImplicitAnyTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Any)

class AstImplicitNullableAnyTypeRef :
    AstImplicitBuiltinTypeRef(StandardClassIds.Any, isNullable = true)

class AstImplicitEnumTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Enum)

class AstImplicitAnnotationTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Annotation)

class AstImplicitBooleanTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Boolean)

class AstImplicitByteTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Byte)

class AstImplicitShortTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Short)

class AstImplicitIntTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Int)

class AstImplicitLongTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Long)

class AstImplicitDoubleTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Double)

class AstImplicitFloatTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Float)

class AstImplicitNothingTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Nothing)

class AstImplicitNullableNothingTypeRef :
    AstImplicitBuiltinTypeRef(StandardClassIds.Nothing, isNullable = true)

class AstImplicitCharTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.Char)

class AstImplicitStringTypeRef : AstImplicitBuiltinTypeRef(StandardClassIds.String)

class AstImplicitKPropertyTypeRef(
    typeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(StandardClassIds.KProperty, arrayOf(typeArgument))

class AstImplicitKProperty0TypeRef(
    propertyTypeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(StandardClassIds.KProperty0, arrayOf(propertyTypeArgument))

class AstImplicitKMutableProperty0TypeRef(
    propertyTypeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(StandardClassIds.KMutableProperty0, arrayOf(propertyTypeArgument))

class AstImplicitKProperty1TypeRef(
    receiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(
    StandardClassIds.KProperty1,
    arrayOf(receiverTypeArgument, propertyTypeArgument)
)

class AstImplicitKMutableProperty1TypeRef(
    receiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(
    StandardClassIds.KMutableProperty1,
    arrayOf(receiverTypeArgument, propertyTypeArgument)
)

class AstImplicitKProperty2TypeRef(
    dispatchReceiverTypeArgument: ConeTypeProjection,
    extensionReceiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(
    StandardClassIds.KProperty2,
    arrayOf(dispatchReceiverTypeArgument, extensionReceiverTypeArgument, propertyTypeArgument)
)

class AstImplicitKMutableProperty2TypeRef(
    dispatchReceiverTypeArgument: ConeTypeProjection,
    extensionReceiverTypeArgument: ConeTypeProjection,
    propertyTypeArgument: ConeTypeProjection
) : AstImplicitBuiltinTypeRef(
    StandardClassIds.KMutableProperty2,
    arrayOf(dispatchReceiverTypeArgument, extensionReceiverTypeArgument, propertyTypeArgument)
)

fun AstImplicitBuiltinTypeRef.withFakeSource(kind: AstFakeSourceElementKind): AstImplicitBuiltinTypeRef {
    val source = source ?: return this
    if (source.kind == kind) return this
    val newSource = source.fakeElement(kind)
    return when (this) {
        is AstImplicitUnitTypeRef -> AstImplicitUnitTypeRef(newSource)
        is AstImplicitAnyTypeRef -> AstImplicitAnyTypeRef(newSource)
        is AstImplicitNullableAnyTypeRef -> AstImplicitNullableAnyTypeRef(newSource)
        is AstImplicitEnumTypeRef -> AstImplicitEnumTypeRef(newSource)
        is AstImplicitAnnotationTypeRef -> AstImplicitAnnotationTypeRef(newSource)
        is AstImplicitBooleanTypeRef -> AstImplicitBooleanTypeRef(newSource)
        is AstImplicitByteTypeRef -> AstImplicitByteTypeRef(newSource)
        is AstImplicitShortTypeRef -> AstImplicitShortTypeRef(newSource)
        is AstImplicitIntTypeRef -> AstImplicitIntTypeRef(newSource)
        is AstImplicitLongTypeRef -> AstImplicitLongTypeRef(newSource)
        is AstImplicitDoubleTypeRef -> AstImplicitDoubleTypeRef(newSource)
        is AstImplicitFloatTypeRef -> AstImplicitFloatTypeRef(newSource)
        is AstImplicitNothingTypeRef -> AstImplicitNothingTypeRef(newSource)
        is AstImplicitNullableNothingTypeRef -> AstImplicitNullableNothingTypeRef(newSource)
        is AstImplicitCharTypeRef -> AstImplicitCharTypeRef(newSource)
        is AstImplicitStringTypeRef -> AstImplicitStringTypeRef(newSource)
        is AstImplicitKPropertyTypeRef -> AstImplicitKPropertyTypeRef(
            newSource,
            typeArgument = type.typeArguments[0]
        )
        is AstImplicitKProperty0TypeRef -> AstImplicitKProperty0TypeRef(
            newSource,
            propertyTypeArgument = type.typeArguments[0]
        )
        is AstImplicitKMutableProperty0TypeRef -> AstImplicitKMutableProperty0TypeRef(
            newSource,
            propertyTypeArgument = type.typeArguments[0]
        )
        is AstImplicitKProperty1TypeRef -> AstImplicitKProperty1TypeRef(
            newSource,
            receiverTypeArgument = type.typeArguments[0],
            propertyTypeArgument = type.typeArguments[1]
        )
        is AstImplicitKMutableProperty1TypeRef -> AstImplicitKMutableProperty1TypeRef(
            newSource,
            receiverTypeArgument = type.typeArguments[0],
            propertyTypeArgument = type.typeArguments[1]
        )
        is AstImplicitKProperty2TypeRef -> AstImplicitKProperty2TypeRef(
            newSource,
            dispatchReceiverTypeArgument = type.typeArguments[0],
            extensionReceiverTypeArgument = type.typeArguments[1],
            propertyTypeArgument = type.typeArguments[2]
        )
        is AstImplicitKMutableProperty2TypeRef -> AstImplicitKMutableProperty2TypeRef(
            newSource,
            dispatchReceiverTypeArgument = type.typeArguments[0],
            extensionReceiverTypeArgument = type.typeArguments[1],
            propertyTypeArgument = type.typeArguments[2]
        )
    }
}
