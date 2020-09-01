package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.StandardClassIds
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.ClassId

class AstImplicitTypeImpl : AstType() {

    override val annotations: List<AstAnnotationCall>
        get() = emptyList()

    override val isMarkedNullable: Boolean
        get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstType {
        return this
    }

}

sealed class AstImplicitBuiltinType(
    val id: ClassId,
    override val isMarkedNullable: Boolean = false
) : AstSimpleType() {

    override val annotations: List<AstAnnotationCall>
        get() = emptyList()

    override val classifier: AstClassifierSymbol<*>
        get() = TODO()

    override val arguments: List<AstTypeProjection>
        get() = emptyList()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleType {
        return this
    }

}

class AstImplicitUnitType : AstImplicitBuiltinType(StandardClassIds.Unit)

class AstImplicitAnyType : AstImplicitBuiltinType(StandardClassIds.Any)

class AstImplicitNullableAnyType :
    AstImplicitBuiltinType(StandardClassIds.Any, isMarkedNullable = true)

class AstImplicitEnumType : AstImplicitBuiltinType(StandardClassIds.Enum)

class AstImplicitAnnotationType : AstImplicitBuiltinType(StandardClassIds.Annotation)

class AstImplicitBooleanType : AstImplicitBuiltinType(StandardClassIds.Boolean)

class AstImplicitByteType : AstImplicitBuiltinType(StandardClassIds.Byte)

class AstImplicitShortType : AstImplicitBuiltinType(StandardClassIds.Short)

class AstImplicitIntType : AstImplicitBuiltinType(StandardClassIds.Int)

class AstImplicitLongType : AstImplicitBuiltinType(StandardClassIds.Long)

class AstImplicitDoubleType : AstImplicitBuiltinType(StandardClassIds.Double)

class AstImplicitFloatType : AstImplicitBuiltinType(StandardClassIds.Float)

class AstImplicitNothingType : AstImplicitBuiltinType(StandardClassIds.Nothing)

class AstImplicitNullableNothingType :
    AstImplicitBuiltinType(StandardClassIds.Nothing, isMarkedNullable = true)

class AstImplicitCharType : AstImplicitBuiltinType(StandardClassIds.Char)

class AstImplicitStringType : AstImplicitBuiltinType(StandardClassIds.String)
