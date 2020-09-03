package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.builder.buildSimpleType
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import com.ivianuu.ast.types.impl.AstStarProjectionImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.upperIfFlexible

class TypeConverter(
    private val annotationGenerator: AnnotationGenerator,
    private val symbolTable: DescriptorSymbolTable
) {

    fun convert(kotlinType: KotlinType): AstSimpleType {
        check(kotlinType is SimpleType) {
            "Unexpected kotlin type $kotlinType"
        }

        val approximatedType = kotlinType.upperIfFlexible()

        return buildSimpleType {
            annotations += approximatedType.annotations.map {
                annotationGenerator.generateAnnotation(it)
            }
            classifier = when (val classifierDescriptor = approximatedType.constructor.declarationDescriptor) {
                is ClassDescriptor -> symbolTable.getClassSymbol(classifierDescriptor)
                is TypeParameterDescriptor -> symbolTable.getTypeParameterSymbol(classifierDescriptor)
                else -> error("Unexpected classifier $classifierDescriptor")
            }
            isMarkedNullable = approximatedType.isMarkedNullable
            arguments += approximatedType.arguments.map { argument ->
                when (argument) {
                    is StarProjectionImpl -> AstStarProjectionImpl
                    else -> buildTypeProjectionWithVariance {
                        type = convert(argument.type)
                        variance = argument.projectionKind
                    }
                }
            }
        }
    }

}
