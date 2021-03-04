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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.resolution.TypeRef
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.withAbbreviation

fun TypeRef.toIrType(
    pluginContext: IrPluginContext,
    declarationStore: DeclarationStore
): IrType =
    pluginContext.typeTranslator.translateType(toKotlinType(declarationStore))
        .also {
            it.classifierOrNull?.let {
                (pluginContext as IrPluginContextImpl)
                    .linker.getDeclaration(it)
            }
        }

fun TypeRef.toKotlinType(declarationStore: DeclarationStore): SimpleType {
    if (isStarProjection) return declarationStore.module.builtIns.anyType
    return if (classifier.isTypeAlias) {
        expandedType!!.toKotlinType(declarationStore)
            .withAbbreviation(toAbbreviation(declarationStore))
    } else {
        classifier.descriptor!!.original.defaultType
            .replace(newArguments = arguments.map {
                TypeProjectionImpl(
                    it.variance,
                    it.toKotlinType(declarationStore)
                )
            })
            .makeComposableAsSpecified(isComposable)
            .makeNullableAsSpecified(isMarkedNullable)
    }
}

fun TypeRef.toAbbreviation(declarationStore: DeclarationStore): SimpleType {
    val defaultType = classifier.descriptor!!.defaultType
    return defaultType
        .replace(newArguments = arguments.map {
            TypeProjectionImpl(
                it.variance,
                it.toKotlinType(declarationStore)
            )
        })

        .makeNullableAsSpecified(isMarkedNullable)
}

private fun SimpleType.makeComposableAsSpecified(isComposable: Boolean): SimpleType {
    return replaceAnnotations(
        if (isComposable) {
            Annotations.create(
                listOf(
                    AnnotationDescriptorImpl(
                        constructor.declarationDescriptor!!.module
                            .findClassAcrossModuleDependencies(
                                ClassId.topLevel(InjektFqNames.Composable)
                        )!!.defaultType,
                        emptyMap(),
                        SourceElement.NO_SOURCE
                    )
                )
            )
        } else {
            Annotations.create(
                annotations.filter {
                    it.type.constructor.declarationDescriptor?.fqNameSafe != InjektFqNames.Composable
                }
            )
        }
    )
}

fun IrType.toKotlinType(): SimpleType {
    this as IrSimpleType
    return makeKotlinType(
        classifier,
        arguments,
        hasQuestionMark,
        annotations,
        abbreviation
    )
}

fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    hasQuestionMark: Boolean,
    annotations: List<IrConstructorCall>,
    abbreviation: IrTypeAbbreviation?,
): SimpleType {
    val kotlinTypeArguments = arguments.mapIndexed { index, it ->
        val typeProjectionBase = when (it) {
            is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
            is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
            else -> error(it)
        }
        typeProjectionBase
    }
    return classifier.descriptor.defaultType
        .replace(
            newArguments = kotlinTypeArguments,
            newAnnotations = if (annotations.isEmpty()) Annotations.EMPTY
            else Annotations.create(annotations.map { it.toAnnotationDescriptor() })
        )
        .makeNullableAsSpecified(hasQuestionMark)
        .let { type ->
            if (abbreviation != null) {
                type.withAbbreviation(
                    abbreviation.typeAlias.descriptor.defaultType
                        .replace(
                            newArguments = abbreviation.arguments.mapIndexed { index, it ->
                                when (it) {
                                    is IrTypeProjection -> TypeProjectionImpl(
                                        it.variance,
                                        it.type.toKotlinType()
                                    )
                                    is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
                                    else -> error(it)
                                }
                            },
                            newAnnotations = if (annotations.isEmpty()) Annotations.EMPTY
                            else Annotations.create(annotations.map { it.toAnnotationDescriptor() })
                        )
                )
            } else {
                type
            }
        }
}

fun IrConstructorCall.toAnnotationDescriptor() = AnnotationDescriptorImpl(
    type.toKotlinType(),
    symbol.owner.valueParameters.map { it.name to getValueArgument(it.index) }
        .filter { it.second != null }
        .associate { it.first to it.second!!.toConstantValue() },
    SourceElement.NO_SOURCE
)

fun IrElement.toConstantValue(): ConstantValue<*> {
    return when (this) {
        is IrConst<*> -> when (kind) {
            IrConstKind.Null -> NullValue()
            IrConstKind.Boolean -> BooleanValue(value as Boolean)
            IrConstKind.Char -> CharValue(value as Char)
            IrConstKind.Byte -> ByteValue(value as Byte)
            IrConstKind.Short -> ShortValue(value as Short)
            IrConstKind.Int -> IntValue(value as Int)
            IrConstKind.Long -> LongValue(value as Long)
            IrConstKind.String -> StringValue(value as String)
            IrConstKind.Float -> FloatValue(value as Float)
            IrConstKind.Double -> DoubleValue(value as Double)
        }
        is IrVararg -> {
            val elements =
                elements.map { if (it is IrSpreadElement) error("$it is not expected") else it.toConstantValue() }
            ArrayValue(elements) { moduleDescriptor ->
                // TODO: substitute.
                moduleDescriptor.builtIns.array.defaultType
            }
        }
        is IrGetEnumValue -> EnumValue(
            symbol.owner.parentAsClass.descriptor.classId!!,
            symbol.owner.name
        )
        is IrClassReference -> KClassValue(
            classType.classifierOrFail.descriptor.classId!!, /*TODO*/
            0
        )
        is IrConstructorCall -> AnnotationValue(this.toAnnotationDescriptor())
        else -> error("$this is not expected: ${this.dump()}")
    }
}

fun IrBuilderWithScope.irLambda(
    type: IrType,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
    parameterNameProvider: (Int) -> String = { "p$it" },
    body: IrBuilderWithScope.(IrFunction) -> IrExpression,
): IrExpression {
    type as IrSimpleType
    val returnType = type.arguments.last().typeOrNull!!

    val lambda = IrFactoryImpl.buildFun {
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        name = Name.special("<anonymous>")
        this.returnType = returnType
        visibility = DescriptorVisibilities.LOCAL
        isSuspend = type.classifier.descriptor.fqNameSafe.asString()
            .startsWith("kotlin.coroutines.SuspendFunction")
    }.apply {
        parent = scope.getLocalDeclarationParent()
        type.arguments.dropLast(1).forEachIndexed { index, typeArgument ->
            addValueParameter(
                parameterNameProvider(index),
                typeArgument.typeOrNull!!
            )
        }
        annotations += type.annotations.map {
            it.deepCopyWithSymbols()
        }
        this.body =
            DeclarationIrBuilder(context, symbol).run {
                irBlockBody {
                    +irReturn(body(this, this@apply))
                }
            }
    }

    return IrFunctionExpressionImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = type,
        function = lambda,
        origin = IrStatementOrigin.LAMBDA
    )
}

fun wrapDescriptor(descriptor: FunctionDescriptor): WrappedSimpleFunctionDescriptor {
    return when (descriptor) {
        is PropertyGetterDescriptor ->
            WrappedPropertyGetterDescriptor()
        is PropertySetterDescriptor ->
            WrappedPropertySetterDescriptor()
        is DescriptorWithContainerSource ->
            WrappedFunctionDescriptorWithContainerSource()
        else -> object : WrappedSimpleFunctionDescriptor() {
            override fun getSource(): SourceElement = descriptor.source
        }
    }
}

fun IrBuilderWithScope.jvmNameAnnotation(
    name: String,
    pluginContext: IrPluginContext
): IrConstructorCall {
    val jvmName = pluginContext.referenceClass(DescriptorUtils.JVM_NAME)!!
    return irCall(jvmName.constructors.single()).apply {
        putValueArgument(0, irString(name))
    }
}

fun IrFunction.copy(pluginContext: IrPluginContext): IrSimpleFunction {
    val descriptor = descriptor
    val newDescriptor = wrapDescriptor(descriptor)

    return IrFunctionImpl(
        startOffset,
        endOffset,
        origin,
        IrSimpleFunctionSymbolImpl(newDescriptor),
        name,
        visibility,
        descriptor.modality,
        returnType,
        isInline,
        isExternal,
        descriptor.isTailrec,
        isSuspend,
        descriptor.isOperator,
        descriptor.isInfix,
        isExpect,
        isFakeOverride,
        containerSource
    ).also { fn ->
        newDescriptor.bind(fn)
        if (this is IrSimpleFunction) {
            val propertySymbol = correspondingPropertySymbol
            if (propertySymbol != null) {
                fn.correspondingPropertySymbol = propertySymbol
                if (propertySymbol.owner.getter == this) {
                    propertySymbol.owner.getter = fn
                }
                if (propertySymbol.owner.setter == this) {
                    propertySymbol.owner.setter = this
                }
            }
        }
        fn.parent = parent
        fn.typeParameters = this.typeParameters.map {
            it.parent = fn
            it
        }

        fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
        fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
        fn.valueParameters = valueParameters.map { p ->
            p.copyTo(fn, name = dexSafeName(p.name))
        }
        fn.annotations = annotations.map { a -> a }
        fn.metadata = metadata
        fn.body = body?.deepCopyWithSymbols(this)
        val parameterMapping = allParameters
            .map { it.symbol }
            .zip(fn.allParameters)
            .toMap()
        fn.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return parameterMapping[expression.symbol]
                    ?.let { DeclarationIrBuilder(pluginContext, fn.symbol).irGet(it) }
                    ?: super.visitGetValue(expression)
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                if (expression.returnTargetSymbol == symbol) {
                    return super.visitReturn(
                        IrReturnImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            fn.symbol,
                            expression.value
                        )
                    )
                }
                return super.visitReturn(expression)
            }
        })
    }
}


private fun dexSafeName(name: Name): Name {
    return if (name.isSpecial && name.asString().contains(' ')) {
        val sanitized = name
            .asString()
            .replace(' ', '$')
            .replace('<', '$')
            .replace('>', '$')
        Name.identifier(sanitized)
    } else name
}
