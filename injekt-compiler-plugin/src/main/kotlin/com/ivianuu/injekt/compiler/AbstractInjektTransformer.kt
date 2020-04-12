/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class AbstractInjektTransformer(
    protected val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        return super.visitModuleFragment(declaration)
            .also { it.patchDeclarationParents() }
    }

    protected val symbolTable = pluginContext.symbolTable
    protected val typeTranslator = pluginContext.typeTranslator
    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

    protected val injektPackage =
        pluginContext.moduleDescriptor.getPackage(InjektClassNames.InjektPackage)
    protected val injektInternalPackage =
        pluginContext.moduleDescriptor.getPackage(InjektClassNames.InjektInternalPackage)

    protected fun getClass(fqName: FqName) =
        pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    protected fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

    protected fun <T : IrSymbol> T.ensureBound(): T {
        if (!this.isBound) pluginContext.irProvider.getDeclaration(this)
        check(this.isBound) { "$this is not bound" }
        return this
    }

    protected fun IrFunction.createParameterDeclarations(descriptor: FunctionDescriptor) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        fun TypeParameterDescriptor.irTypeParameter() = IrTypeParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrTypeParameterSymbolImpl(this)
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty()) { "params ${valueParameters.map { it.name }}" }
        valueParameters = descriptor.valueParameters.map { it.irValueParameter() }

        assert(typeParameters.isEmpty()) { "types ${typeParameters.map { it.name }}" }
        typeParameters + descriptor.typeParameters.map { it.irTypeParameter() }
    }

    protected fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    protected fun IrBuilderWithScope.irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)

        val returnType = descriptor.returnType!!.toIrType()

        val lambda = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            symbol,
            returnType
        ).also {
            it.parent = scope.getLocalDeclarationParent()
            it.createParameterDeclarations(descriptor)
            it.body = DeclarationIrBuilder(this@AbstractInjektTransformer.pluginContext, symbol)
                .irBlockBody { body(it) }
        }

        return IrFunctionExpressionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = type,
            origin = IrStatementOrigin.LAMBDA,
            function = lambda
        )
    }

    protected fun IrBuilderWithScope.syntheticAnnotationAccessor(
        annotation: AnnotationDescriptor
    ): IrExpression {
        return when (val declaration =
            annotation.getDeclarationForSyntheticAnnotation(pluginContext.moduleDescriptor)) {
            is ClassDescriptor -> {
                if (declaration.kind == ClassKind.OBJECT) {
                    irGetObject(symbolTable.referenceClass(declaration))
                } else {
                    val constructor = declaration.unsubstitutedPrimaryConstructor!!
                    irCall(
                        symbolTable.referenceConstructor(constructor as ClassConstructorDescriptor),
                        declaration.defaultType.toIrType()
                    ).apply {
                        annotation.type.arguments.forEachIndexed { index, typeArgument ->
                            putTypeArgument(
                                index,
                                typeArgument.type.toIrType()
                            )
                        }

                        annotation.allValueArguments.toList().forEach { (name, value) ->
                            putValueArgument(
                                constructor.valueParameters.single { it.name == name }.index,
                                generateConstantOrAnnotationValueAsExpression(constantValue = value)
                            )
                        }
                    }
                }
            }
            is FunctionDescriptor -> {
                irCall(
                    symbolTable.referenceSimpleFunction(declaration),
                    declaration.returnType!!.toIrType()
                ).apply {
                    annotation.type.arguments.forEachIndexed { index, typeArgument ->
                        putTypeArgument(
                            index,
                            typeArgument.type.toIrType()
                        )
                    }

                    annotation.allValueArguments.toList().forEach { (name, value) ->
                        putValueArgument(
                            declaration.valueParameters.single { it.name == name }.index,
                            generateConstantOrAnnotationValueAsExpression(constantValue = value)
                        )
                    }
                }
            }
            is PropertyDescriptor -> {
                irCall(
                    symbolTable.referenceSimpleFunction(declaration.getter!!),
                    declaration.type.toIrType()
                )
            }
            else -> error("Unexpected declaration for $annotation -> $declaration")
        }
    }

    private fun generateConstantOrAnnotationValueAsExpression(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): IrExpression? {
        val constantKtType = constantValue.getType(pluginContext.moduleDescriptor)
        val constantType = constantKtType.toIrType()

        return when (constantValue) {
            is StringValue -> IrConstImpl.string(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is IntValue -> IrConstImpl.int(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is UIntValue -> IrConstImpl.int(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is NullValue -> IrConstImpl.constNull(startOffset, endOffset, constantType)
            is BooleanValue -> IrConstImpl.boolean(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is LongValue -> IrConstImpl.long(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is ULongValue -> IrConstImpl.long(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is DoubleValue -> IrConstImpl.double(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is FloatValue -> IrConstImpl.float(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is CharValue -> IrConstImpl.char(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is ByteValue -> IrConstImpl.byte(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is UByteValue -> IrConstImpl.byte(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is ShortValue -> IrConstImpl.short(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is UShortValue -> IrConstImpl.short(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )

            is ArrayValue -> {
                val arrayElementType =
                    varargElementType ?: pluginContext.builtIns.getArrayElementType(constantKtType)
                IrVarargImpl(
                    startOffset, endOffset,
                    constantType,
                    arrayElementType.toIrType(),
                    constantValue.value.mapNotNull {
                        generateConstantOrAnnotationValueAsExpression(
                            startOffset,
                            endOffset,
                            it,
                            null
                        )
                    }
                )
            }

            is EnumValue -> {
                val enumEntryDescriptor =
                    constantKtType.memberScope.getContributedClassifier(
                        constantValue.enumEntryName,
                        NoLookupLocation.FROM_BACKEND
                    )
                        ?: throw AssertionError("No such enum entry ${constantValue.enumEntryName} in $constantType")
                if (enumEntryDescriptor !is ClassDescriptor) {
                    throw AssertionError("Enum entry $enumEntryDescriptor should be a ClassDescriptor")
                }
                if (!DescriptorUtils.isEnumEntry(enumEntryDescriptor)) {
                    // Error class descriptor for an unresolved entry.
                    // TODO this `null` may actually reach codegen if the annotation is on an interface member's default implementation,
                    //      as any bridge generated in an implementation of that interface will have a copy of the annotation. See
                    //      `missingEnumReferencedInAnnotationArgumentIr` in `testData/compileKotlinAgainstCustomBinaries`: replace
                    //      `open class B` with `interface B` and watch things break. (`KClassValue` below likely has a similar problem.)
                    return null
                }
                IrGetEnumValueImpl(
                    startOffset, endOffset,
                    constantType,
                    symbolTable.referenceEnumEntry(enumEntryDescriptor)
                )
            }

            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value)

            is KClassValue -> {
                val classifierKtType = constantValue.getArgumentType(pluginContext.moduleDescriptor)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                    IrClassReferenceImpl(
                        startOffset, endOffset,
                        constantValue.getType(pluginContext.moduleDescriptor).toIrType(),
                        symbolTable.referenceClassifier(classifierDescriptor),
                        classifierKtType.toIrType()
                    )
                }
            }

            is ErrorValue -> null

            else -> TODO("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    private fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): IrConstructorCall? {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor
        if (annotationClassDescriptor !is ClassDescriptor) return null
        if (annotationClassDescriptor is NotFoundClasses.MockClassDescriptor) return null

        assert(DescriptorUtils.isAnnotationClass(annotationClassDescriptor)) {
            "Annotation class expected: $annotationClassDescriptor"
        }

        val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
            ?: annotationClassDescriptor.constructors.singleOrNull()
            ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")
        val primaryConstructorSymbol =
            symbolTable.referenceConstructor(primaryConstructorDescriptor)

        val psi = annotationDescriptor.source.safeAs<PsiSourceElement>()?.psi
        val startOffset =
            psi?.takeUnless { it.containingFile.fileType.isBinary }?.startOffset ?: UNDEFINED_OFFSET
        val endOffset =
            psi?.takeUnless { it.containingFile.fileType.isBinary }?.endOffset ?: UNDEFINED_OFFSET

        val irCall = IrConstructorCallImpl.fromSymbolDescriptor(
            startOffset, endOffset,
            annotationType.toIrType(),
            primaryConstructorSymbol
        )

        for (valueParameter in primaryConstructorDescriptor.valueParameters) {
            val argumentIndex = valueParameter.index
            val argumentValue =
                annotationDescriptor.allValueArguments[valueParameter.name] ?: continue
            val irArgument = generateConstantOrAnnotationValueAsExpression(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                argumentValue,
                valueParameter.varargElementType
            )
            if (irArgument != null) {
                irCall.putValueArgument(argumentIndex, irArgument)
            }
        }

        return irCall
    }

    protected fun IrBuilderWithScope.createFunctionDescriptor(
        type: KotlinType,
        owner: DeclarationDescriptor = scope.scopeOwner
    ): FunctionDescriptor {
        return AnonymousFunctionDescriptor(
            owner,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            initialize(
                type.getReceiverTypeFromFunctionType()?.let {
                    DescriptorFactory.createExtensionReceiverParameterForCallable(
                        this,
                        it,
                        Annotations.EMPTY
                    )
                },
                null,
                emptyList(),
                type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                    ValueParameterDescriptorImpl(
                        containingDeclaration = this,
                        original = null,
                        index = i,
                        annotations = Annotations.EMPTY,
                        name = t.type.extractParameterNameFromFunctionTypeArgument()
                            ?: Name.identifier("p$i"),
                        outType = t.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = null,
                        source = SourceElement.NO_SOURCE
                    )
                },
                type.getReturnTypeFromFunctionType(),
                Modality.FINAL,
                Visibilities.LOCAL,
                null
            )
            isOperator = false
            isInfix = false
            isExternal = false
            isInline = false
            isTailrec = false
            isSuspend = false
            isExpect = false
            isActual = false
        }
    }

}

object InjektOrigin : IrDeclarationOrigin
