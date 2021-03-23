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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.toMap
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
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
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.withAbbreviation

fun TypeRef.toIrType(
    pluginContext: IrPluginContext,
    localClasses: List<IrClass>,
    context: InjektContext
): IrType {
    if (isStarProjection) return pluginContext.irBuiltIns.anyType
    return if (classifier.isTypeAlias) {
        superTypes.single()
            .toIrType(pluginContext, localClasses, context)
            .let {
                it as IrSimpleType
                IrSimpleTypeImpl(
                    it.classifier,
                    it.hasQuestionMark,
                    it.arguments,
                    it.annotations,
                    toIrAbbreviation(pluginContext, localClasses, context)
                )
            }
    } else {
        val key = classifier.descriptor!!.uniqueKey(context)
        val fqName = FqName(key.split(":")[1])
        val irClassifier = localClasses.singleOrNull { it.descriptor.fqNameSafe == fqName }
            ?.symbol
            ?: pluginContext.referenceClass(fqName)
            ?: pluginContext.referenceFunctions(fqName.parent())
                .flatMap { it.owner.typeParameters }
                .singleOrNull { it.descriptor.uniqueKey(context) == key }
                ?.symbol
            ?: pluginContext.referenceProperties(fqName.parent())
                .flatMap { it.owner.getter!!.typeParameters }
                .singleOrNull { it.descriptor.uniqueKey(context) == key }
                ?.symbol
            ?: (pluginContext.referenceClass(fqName.parent())
                ?: pluginContext.referenceTypeAlias(fqName.parent()))
                ?.owner
                ?.typeParameters
                ?.singleOrNull { it.descriptor.uniqueKey(context) == key }
                ?.symbol
            ?: error("Could not get for $fqName $key")
        IrSimpleTypeImpl(
            irClassifier,
            isMarkedNullable,
            arguments.map { makeTypeProjection(it.toIrType(pluginContext, localClasses, context), Variance.INVARIANT) },
            listOfNotNull(
                qualifier
                    ?.toIrType(pluginContext, localClasses, context)
                    ?.let {
                        DeclarationIrBuilder(pluginContext, it.classifierOrFail)
                            .irCall(it.classOrNull!!.owner.constructors.single().symbol, it, it.classOrNull!!.owner)
                    }
            ),
            null
        ).makeComposableAsSpecified(pluginContext, context, isMarkedComposable)
    }
}

private fun TypeRef.toIrAbbreviation(
    pluginContext: IrPluginContext,
    localClasses: List<IrClass>,
    context: InjektContext
): IrTypeAbbreviation {
    val typeAlias = pluginContext.referenceTypeAlias(classifier.fqName)!!
    return IrTypeAbbreviationImpl(
        typeAlias,
        isMarkedComposable,
        arguments.map {
            makeTypeProjection(it.toIrType(pluginContext, localClasses, context), Variance.INVARIANT)
        },
        listOfNotNull(
            qualifier
                ?.toIrType(pluginContext, localClasses, context)
                ?.let {
                    DeclarationIrBuilder(pluginContext, it.classifierOrFail)
                        .irCall(it.classOrNull!!.owner.constructors.single().symbol, it, it.classOrNull!!.owner)
                }
        )
    )
}

private fun IrSimpleType.makeComposableAsSpecified(
    pluginContext: IrPluginContext,
    context: InjektContext,
    isComposable: Boolean
): IrSimpleType {
    val newAnnotations = if (isComposable) {
        if (annotations.any { it.type.classOrNull?.descriptor?.fqNameSafe == InjektFqNames.Composable }) {
            annotations
        } else {
            val composableConstructor = pluginContext.referenceConstructors(InjektFqNames.Composable)
                .single()
            annotations + DeclarationIrBuilder(pluginContext, composableConstructor)
                .irCall(composableConstructor)
        }
    } else {
        annotations.filter {
            it.type.classOrNull?.descriptor?.fqNameSafe != InjektFqNames.Composable
        }
    }
    return IrSimpleTypeImpl(
        classifier,
        hasQuestionMark,
        arguments,
        newAnnotations,
        abbreviation
    )
}

fun TypeRef.toKotlinType(context: InjektContext): SimpleType {
    if (isStarProjection) return context.module.builtIns.anyType
    return if (classifier.isTypeAlias) {
        superTypes.single().toKotlinType(context)
            .withAbbreviation(toAbbreviation(context))
    } else {
        classifier.descriptor!!.original.defaultType
            .replace(newArguments = arguments.map {
                TypeProjectionImpl(
                    Variance.INVARIANT,
                    it.toKotlinType(context)
                )
            })
            .makeComposableAsSpecified(context, isMarkedComposable)
            .makeNullableAsSpecified(isMarkedNullable)
    }
}

fun TypeRef.toAbbreviation(context: InjektContext): SimpleType {
    val defaultType = classifier.descriptor!!.defaultType
    return defaultType
        .replace(newArguments = arguments.map {
            TypeProjectionImpl(
                Variance.INVARIANT,
                it.toKotlinType(context)
            )
        })

        .makeNullableAsSpecified(isMarkedNullable)
}

private fun SimpleType.makeComposableAsSpecified(
    context: InjektContext,
    isComposable: Boolean
): SimpleType {
    return replaceAnnotations(
        if (isComposable) {
            Annotations.create(
                listOf(
                    AnnotationDescriptorImpl(
                        context.classifierDescriptorForFqName(InjektFqNames.Composable)!!.defaultType,
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
            .toMap(fn.allParameters)
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
