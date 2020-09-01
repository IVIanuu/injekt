package com.ivianuu.ast.psi

import com.ivianuu.ast.AstGeneratorContext
import com.ivianuu.ast.tree.declaration.AstClass
import com.ivianuu.ast.tree.declaration.AstFunction
import com.ivianuu.ast.tree.expression.AstConst
import com.ivianuu.ast.tree.expression.AstExpression
import com.ivianuu.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.ast.tree.expression.AstVararg
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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

fun AstGeneratorContext.generateConstantValueAsExpression(
    constantValue: ConstantValue<*>
): AstExpression =
    // Assertion is safe here because annotation calls and class literals are not allowed in constant initializers
    generateConstantOrAnnotationValueAsExpression(constantValue)!!

fun AstGeneratorContext.generateConstantOrAnnotationValueAsExpression(
    constantValue: ConstantValue<*>
): AstExpression? {
    val constantKtType = constantValue.getType(module)
    val constantType = typeMapper.translate(constantKtType)

    return when (constantValue) {
        is StringValue -> AstConst.string(constantType, constantValue.value)
        is IntValue -> AstConst.int(constantType, constantValue.value)
        is UIntValue -> AstConst.int(constantType, constantValue.value)
        is NullValue -> AstConst.constNull(constantType)
        is BooleanValue -> AstConst.boolean(constantType, constantValue.value)
        is LongValue -> AstConst.long(constantType, constantValue.value)
        is ULongValue -> AstConst.long(constantType, constantValue.value)
        is DoubleValue -> AstConst.double(constantType, constantValue.value)
        is FloatValue -> AstConst.float(constantType, constantValue.value)
        is CharValue -> AstConst.char(constantType, constantValue.value)
        is ByteValue -> AstConst.byte(constantType, constantValue.value)
        is UByteValue -> AstConst.byte(constantType, constantValue.value)
        is ShortValue -> AstConst.short(constantType, constantValue.value)
        is UShortValue -> AstConst.short(constantType, constantValue.value)
        is ArrayValue -> {
            AstVararg(constantType).apply {
                elements += constantValue.value.mapNotNull {
                    generateConstantOrAnnotationValueAsExpression(it)
                }
            }
        }
        is EnumValue -> {
            val enumEntryDescriptor =
                constantKtType.memberScope.getContributedClassifier(
                    constantValue.enumEntryName,
                    NoLookupLocation.FROM_BACKEND
                )!!
            AstQualifiedAccess(
                callee = provider.get(enumEntryDescriptor) as AstClass,
                type = constantType
            )
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

fun AstGeneratorContext.generateAnnotationConstructorCall(
    annotationDescriptor: AnnotationDescriptor
): AstQualifiedAccess? {
    val annotationType = annotationDescriptor.type
    val annotationClassDescriptor = annotationType.constructor.declarationDescriptor
    if (annotationClassDescriptor !is ClassDescriptor) return null
    if (annotationClassDescriptor is NotFoundClasses.MockClassDescriptor) return null

    val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
        ?: annotationClassDescriptor.constructors.singleOrNull()
        ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")
    val astPrimaryConstructor =
        provider.get<AstFunction>(primaryConstructorDescriptor)

    return AstQualifiedAccess(
        callee = astPrimaryConstructor,
        type = typeMapper.translate(annotationType)
    ).apply {
        valueArguments += primaryConstructorDescriptor.valueParameters
            .map { valueParameter ->
                annotationDescriptor.allValueArguments[valueParameter.name]
                    ?.let { generateConstantOrAnnotationValueAsExpression(it) }
            }
    }
}
