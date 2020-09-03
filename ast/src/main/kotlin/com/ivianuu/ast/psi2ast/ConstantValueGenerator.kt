package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.buildConst
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildVararg
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
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

class ConstantValueGenerator(
    private val module: ModuleDescriptor,
    private val symbolTable: DescriptorSymbolTable,
    private val typeConverter: TypeConverter
) {

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
            is StringValue -> buildConst(constantType, AstConstKind.String, constantValue.value)
            is IntValue -> buildConst(constantType, AstConstKind.Int, constantValue.value)
            is UIntValue -> buildConst(constantType, AstConstKind.Int, constantValue.value)
            is NullValue -> buildConst(constantType, AstConstKind.Null, null)
            is BooleanValue -> buildConst(constantType, AstConstKind.Boolean, constantValue.value)
            is LongValue -> buildConst(constantType, AstConstKind.Long, constantValue.value)
            is ULongValue -> buildConst(constantType, AstConstKind.Long, constantValue.value)
            is DoubleValue -> buildConst(constantType, AstConstKind.Double, constantValue.value)
            is FloatValue -> buildConst(constantType, AstConstKind.Float, constantValue.value)
            is CharValue -> buildConst(constantType, AstConstKind.Char, constantValue.value)
            is ByteValue -> buildConst(constantType, AstConstKind.Byte, constantValue.value)
            is UByteValue -> buildConst(constantType, AstConstKind.Byte, constantValue.value)
            is ShortValue -> buildConst(constantType, AstConstKind.Short, constantValue.value)
            is UShortValue -> buildConst(constantType, AstConstKind.Short, constantValue.value)
            is ArrayValue -> buildVararg {
                type = constantType
                elements += constantValue.value.mapNotNull {
                    generateConstantOrAnnotationValueAsExpression(it)
                }
            }
            is EnumValue -> {
                /*val enumEntryDescriptor =
                    constantKtType.memberScope.getContributedClassifier(
                        constantValue.enumEntryName,
                        NoLookupLocation.FROM_BACKEND
                    )!!
                 {
                    this.calleeReference
                }
                AstQualifiedAccess(
                    callee = provider.get(enumEntryDescriptor) as AstClass,
                    type = constantType
                )*/
                TODO()
            }
            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value)
            is KClassValue -> {
                /*val classifierKtType = constantValue.getArgumentType(moduleDescriptor)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")
                    IrClassReferenceImpl(
                        startOffset, endOffset,
                        constantValue.getType(moduleDescriptor).toIrType(),
                        symbolTable.referenceClassifier(classifierDescriptor),
                        classifierKtType.toIrType()
                    )
                }*/ TODO()
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

        return buildFunctionCall {
            type = typeConverter.convert(annotationType)
            callee = symbolTable.getConstructorSymbol(primaryConstructorDescriptor)
            valueArguments += primaryConstructorDescriptor.valueParameters
                .map { valueParameter ->
                    annotationDescriptor.allValueArguments[valueParameter.name]
                        ?.let { generateConstantOrAnnotationValueAsExpression(it) }
                }
        }
    }

}
