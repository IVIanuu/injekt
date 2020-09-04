package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.builder.buildSimpleType
import com.ivianuu.ast.types.builder.buildStarProjection
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import com.ivianuu.ast.types.impl.AstStarProjectionImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.upperIfFlexible

class TypeConverter(private val symbolTable: DescriptorSymbolTable) {

    lateinit var builder: AstBuilder
    lateinit var constantValueGenerator: ConstantValueGenerator

    fun convert(kotlinType: KotlinType): AstSimpleType {
        val approximatedType = kotlinType.upperIfFlexible()

        return builder.buildSimpleType {
            annotations += approximatedType.annotations.map {
                constantValueGenerator.generateAnnotationConstructorCall(it)!!
            }
            classifier = when (val classifierDescriptor = approximatedType.constructor.declarationDescriptor) {
                is ClassDescriptor -> symbolTable.getClassSymbol(classifierDescriptor)
                is TypeParameterDescriptor -> symbolTable.getTypeParameterSymbol(classifierDescriptor)
                else -> error("Unexpected classifier $classifierDescriptor")
            }
            isMarkedNullable = approximatedType.isMarkedNullable
            arguments += approximatedType.arguments.map { argument ->
                when (argument) {
                    is StarProjectionImpl -> buildStarProjection()
                    else -> buildTypeProjectionWithVariance {
                        type = convert(argument.type)
                        variance = argument.projectionKind
                    }
                }
            }
        }
    }

}
