package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.type.AstClassifier
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeAbbreviation
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjectionImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.upperIfFlexible

class TypeMapper(
    private val astProvider: AstProvider,
    private val storage: Psi2AstStorage
) {

    fun translate(kotlinType: KotlinType): AstType {
        storage.types[kotlinType]?.let { return it }
        val approximatedType = kotlinType.upperIfFlexible()
        return AstType().apply {
            storage.types[kotlinType] = this
            // todo annotations
            classifier =
                when (val classifier = approximatedType.constructor.declarationDescriptor) {
                    is ClassDescriptor -> astProvider.get(classifier) as AstClassifier
                    is TypeParameterDescriptor -> astProvider.get(classifier) as AstTypeParameter
                    else -> error("Unexpected classifier $classifier")
                }
            hasQuestionMark = approximatedType.isMarkedNullable
            arguments += approximatedType.arguments.map { translateTypeArgument(it) }
            abbreviation = approximatedType.getAbbreviation()?.let {
                translateAbbreviation(it)
            }
        }
    }

    private fun translateAbbreviation(abbreviation: SimpleType): AstTypeAbbreviation {
        storage.typeAbbreviations[abbreviation]?.let { return it }
        val typeAliasDescriptor = abbreviation.constructor.declarationDescriptor.let {
            it as? TypeAliasDescriptor
                ?: throw AssertionError("TypeAliasDescriptor expected: $it")
        }
        return AstTypeAbbreviation(astProvider.get(typeAliasDescriptor) as AstTypeAlias).apply {
            hasQuestionMark = abbreviation.isMarkedNullable
            arguments += abbreviation.arguments.map { translateTypeArgument(it) }
            // todo annotations
        }
    }

    private fun translateTypeArgument(typeArgument: TypeArgumentMarker): AstTypeArgument {
        storage.typeArguments[typeArgument]?.let { return it }
        return when (typeArgument) {
            is StarProjectionImpl -> AstStarProjection
            is TypeProjection -> mapTypeProjection(typeArgument)
            else -> error("Unexpected type $this")
        }
    }

    private fun mapTypeProjection(typeProjection: TypeProjection): AstTypeProjection {
        storage.typeProjections[typeProjection]?.let { return it }
        return AstTypeProjectionImpl(
            variance = typeProjection.projectionKind.toAstVariance(),
            type = translate(typeProjection.type)
        )
    }

}
