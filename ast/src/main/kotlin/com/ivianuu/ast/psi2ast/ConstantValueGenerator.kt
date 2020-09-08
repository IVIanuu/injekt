package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.buildConstBoolean
import com.ivianuu.ast.expressions.buildConstByte
import com.ivianuu.ast.expressions.buildConstChar
import com.ivianuu.ast.expressions.buildConstDouble
import com.ivianuu.ast.expressions.buildConstFloat
import com.ivianuu.ast.expressions.buildConstInt
import com.ivianuu.ast.expressions.buildConstLong
import com.ivianuu.ast.expressions.buildConstNull
import com.ivianuu.ast.expressions.buildConstShort
import com.ivianuu.ast.expressions.buildConstString
import com.ivianuu.ast.expressions.buildConstUByte
import com.ivianuu.ast.expressions.buildConstUInt
import com.ivianuu.ast.expressions.buildConstULong
import com.ivianuu.ast.expressions.buildConstUShort
import com.ivianuu.ast.expressions.builder.buildClassReference
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildVararg
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.UByteValue
import org.jetbrains.kotlin.resolve.constants.UIntValue
import org.jetbrains.kotlin.resolve.constants.ULongValue
import org.jetbrains.kotlin.resolve.constants.UShortValue
import org.jetbrains.kotlin.types.isError

class ConstantValueGenerator(
    private val module: ModuleDescriptor,
    private val symbolTable: DescriptorSymbolTable,
    private val typeConverter: TypeConverter
) {

    lateinit var builder: AstBuilder

    fun generateConstantValueAsExpression(
        constantValue: ConstantValue<*>
    ): AstExpression =
        // Assertion is safe here because annotation calls and class literals are not allowed in constant initializers
        generateConstantOrAnnotationValueAsExpression(constantValue)!!

    fun generateConstantOrAnnotationValueAsExpression(
        constantValue: ConstantValue<*>
    ): AstExpression? {
        val constantKtType = constantValue.getType(module)
        val constantType = typeConverter.convert(constantKtType)

        return when (constantValue) {
            is StringValue -> builder.buildConstString(constantValue.value)
            is IntValue -> builder.buildConstInt(constantValue.value)
            is UIntValue -> builder.buildConstUInt(constantValue.value.toUInt())
            is NullValue -> builder.buildConstNull()
            is BooleanValue -> builder.buildConstBoolean(constantValue.value)
            is LongValue -> builder.buildConstLong(constantValue.value)
            is ULongValue -> builder.buildConstULong(constantValue.value.toULong())
            is DoubleValue -> builder.buildConstDouble(constantValue.value)
            is FloatValue -> builder.buildConstFloat(constantValue.value)
            is CharValue -> builder.buildConstChar(constantValue.value)
            is ByteValue -> builder.buildConstByte(constantValue.value)
            is UByteValue -> builder.buildConstUByte(constantValue.value.toUByte())
            is ShortValue -> builder.buildConstShort(constantValue.value)
            is UShortValue -> builder.buildConstUShort(constantValue.value.toUShort())
            is ArrayValue -> builder.buildVararg {
                type = constantType
                elements += constantValue.value.mapNotNull {
                    generateConstantOrAnnotationValueAsExpression(it)
                }
            }
            is EnumValue -> {
                val enumEntryDescriptor =
                    constantKtType.memberScope.getContributedClassifier(
                        constantValue.enumEntryName,
                        NoLookupLocation.FROM_BACKEND
                    )!!
                builder.buildQualifiedAccess {
                    type = constantType
                    callee = symbolTable.getSymbol(enumEntryDescriptor)
                }
            }
            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value)
            is KClassValue -> {
                val classifierKtType = constantValue.getArgumentType(module)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")
                    builder.buildClassReference {
                        type = constantValue.getType(module).let { typeConverter.convert(it) }
                        classifier = symbolTable.getSymbol(classifierDescriptor as ClassDescriptor)
                    }
                }
            }
            is ErrorValue -> null
            else -> error("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): AstFunctionCall? {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor
        if (annotationClassDescriptor !is ClassDescriptor) return null
        if (annotationClassDescriptor is NotFoundClasses.MockClassDescriptor) return null

        val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
            ?: annotationClassDescriptor.constructors.singleOrNull()
            ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")

        return builder.buildFunctionCall {
            type = typeConverter.convert(annotationType)
            callee = symbolTable.getSymbol(primaryConstructorDescriptor)
            valueArguments += primaryConstructorDescriptor.valueParameters
                .map { valueParameter ->
                    annotationDescriptor.allValueArguments[valueParameter.name]
                        ?.let { generateConstantOrAnnotationValueAsExpression(it) }
                }
        }
    }

}

public annotation class A(public val xs: Array<kotlin.String>)  {
}
