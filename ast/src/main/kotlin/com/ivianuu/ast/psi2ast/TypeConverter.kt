package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.builder.buildStarProjection
import com.ivianuu.ast.types.builder.buildType
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.kotlin.types.upperIfFlexible

class TypeConverter(
    private val builtIns: KotlinBuiltIns,
    private val languageVersionSettings: LanguageVersionSettings,
    private val symbolTable: DescriptorSymbolTable
) {

    lateinit var builder: AstBuilder
    lateinit var constantValueGenerator: ConstantValueGenerator

    private val typeApproximatorForNI = TypeApproximator(builtIns)

    fun convert(kotlinType: KotlinType): AstType {
        val flexibleApproximatedType = approximate(kotlinType)
        val approximatedType = flexibleApproximatedType.upperIfFlexible()

        return builder.buildType {
            annotations += approximatedType.annotations.map {
                constantValueGenerator.generateAnnotationConstructorCall(it)!!
            }
            classifier = symbolTable.getSymbol(approximatedType.constructor.declarationDescriptor!!)
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

    private fun approximate(ktType: KotlinType): KotlinType {
        val properlyApproximatedType = approximateByKotlinRules(ktType)

        // If there's an intersection type, take the most common supertype of its intermediate supertypes.
        // That's what old back-end effectively does.
        val typeConstructor = properlyApproximatedType.constructor
        if (typeConstructor is IntersectionTypeConstructor) {
            val commonSupertype = CommonSupertypes.commonSupertype(typeConstructor.supertypes)
            return approximate(commonSupertype.replaceArgumentsWithStarProjections())
        }

        // Assume that other types are approximated properly.
        return properlyApproximatedType
    }

    private fun approximateByKotlinRules(ktType: KotlinType): KotlinType {
        return if (ktType.constructor.isDenotable && ktType.arguments.isEmpty())
            ktType
        else
            typeApproximatorForNI.approximateDeclarationType(
                ktType,
                local = false,
                languageVersionSettings = languageVersionSettings
            )
    }

}
