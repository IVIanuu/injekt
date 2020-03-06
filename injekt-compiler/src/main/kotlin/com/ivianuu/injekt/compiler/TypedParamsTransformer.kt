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
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class TypeOfParamsTransformer(
    private val context: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper
) : IrElementTransformerVoid() {

    private val transformedFunctions: MutableMap<IrFunction, IrFunction> = mutableMapOf()
    private val transformedFunctionSet = mutableSetOf<IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)

        /*declaration.acceptVoid(symbolRemapper)

        val typeRemapper = TypedTypeRemapper(
            context,
            symbolRemapper,
            context.typeTranslator,
            composerTypeDescriptor
        )
        // for each declaration, we create a deepCopy transformer It is important here that we
        // use the "preserving metadata" variant since we are using this copy to *replace* the
        // originals, or else the module we would produce wouldn't have any metadata in it.
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            context,
            symbolRemapper,
            typeRemapper,
            context.typeTranslator
        ).also { typeRemapper.deepCopy = it }
        declaration.transformChildren(
            transformer,
            null
        )
        // just go through and patch all of the parents to make sure things are properly wired
        // up.
        declaration.patchDeclarationParents()*/

        return declaration
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration.withTypeParamIfNeeded())
    }

    override fun visitFile(declaration: IrFile): IrFile {
        val originalFunctions = mutableListOf<IrFunction>()
        val originalProperties = mutableListOf<Pair<IrProperty, IrSimpleFunction>>()
        loop@for (child in declaration.declarations) {
            when (child) {
                is IrFunction -> originalFunctions.add(child)
                is IrProperty -> {
                    val getter = child.getter ?: continue@loop
                    originalProperties.add(child to getter)
                }
            }
        }
        val result = super.visitFile(declaration)
        result.patchWithSyntheticTypeOfDecoys(originalFunctions, originalProperties)

        //declaration.remapComposableTypesWithComposerParam()

        return result
    }

    /*private fun IrCall.withTypeParamIfNeeded(composerParam: IrValueParameter): IrCall {
        val isComposableLambda = isComposableLambdaInvoke()
        if (!symbol.descriptor.isComposable() && !isComposableLambda)
            return this
        val ownerFn = when {
            isComposableLambda ->
                (symbol.owner as IrSimpleFunction).lambdaInvokeWithComposerParamIfNeeded()
            else -> (symbol.owner as IrSimpleFunction).withTypeParamIfNeeded()
        }
        if (!transformedFunctionSet.contains(ownerFn))
            return this
        if (symbol.owner == ownerFn)
            return this
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol,
            typeArgumentsCount,
            valueArgumentsCount + 1, // +1 for the composer param
            origin,
            superQualifierSymbol
        ).also {
            it.copyAttributes(this)
            context.irTrace.record(
                ComposeWritableSlices.IS_COMPOSABLE_CALL,
                it,
                true
            )
            it.copyTypeArgumentsFrom(this)
            it.dispatchReceiver = dispatchReceiver
            it.extensionReceiver = extensionReceiver
            for (i in 0 until valueArgumentsCount) {
                it.putValueArgument(i, getValueArgument(i))
            }
            it.putValueArgument(
                valueArgumentsCount,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    composerParam.symbol
                )
            )
        }
    }*/

    // Transform `@Typed inline fun <reified T> foo(params): RetType` into `inline fun <reified T> foo(params, $type1: Type<T>): RetType`
    private fun IrFunction.withTypeParamIfNeeded(): IrFunction {
        // don't transform functions that themselves were produced by this function. (ie, if we
        // call this with a function that has the synthetic composer parameter, we don't want to
        // transform it further).
        if (transformedFunctionSet.contains(this)) return this

        if (origin == TYPE_OF_DECOY_IMPL) return this

        // if not a typed fn, nothing we need to do
        if (!descriptor.annotations.hasAnnotation(InjektClassNames.Typed) &&
            annotations.none {
                it.symbol.descriptor.returnType == getTopLevelClass(InjektClassNames.Typed).defaultType
            }) return this

        // we don't bother transforming expect functions. They exist only for type resolution and
        // don't need to be transformed to have a composer parameter
        if (isExpect) return this

        // cache the transformed function with composer parameter
        return transformedFunctions[this] ?: copyWithTypeParams()
    }

    private fun IrDeclarationContainer.patchWithSyntheticTypeOfDecoys(
        originalFunctions: List<IrFunction>,
        originalProperties: List<Pair<IrProperty, IrSimpleFunction>>
    ) {
        for (function in originalFunctions) {
            if (transformedFunctions.containsKey(function) && (
                        function.annotations.hasAnnotation(InjektClassNames.Typed) ||
                                function.descriptor.annotations.hasAnnotation(InjektClassNames.Typed)
                        )) {
                declarations.add(function.copyAsTypeOfDecoy())
            }
        }
        for ((property, getter) in originalProperties) {
            if (transformedFunctions.containsKey(getter) && (
                        property.annotations.hasAnnotation(InjektClassNames.Typed) ||
                                property.descriptor.annotations.hasAnnotation(InjektClassNames.Typed)
                        )) {
                val newGetter = property.getter
                assert(getter !== newGetter)
                assert(newGetter != null)
                // NOTE(lmr): the compiler seems to turn a getter with a single parameter into a
                // setter, even though it's in the "getter" position. As a result, we will put
                // the original parameter-less getter in the "getter" position, and add the
                // single-parameter getter to the class itself.
                property.getter = getter.copyAsTypeOfDecoy().also { it.parent = this }
                declarations.add(newGetter!!)
                newGetter.parent = this
            }
        }
    }

    private fun IrFunction.copyAsTypeOfDecoy(): IrSimpleFunction {
        if (origin == IrDeclarationOrigin.FAKE_OVERRIDE) return this as IrSimpleFunction
        return copy().also { fn ->
            fn.origin = TYPE_OF_DECOY_IMPL
            (fn as IrFunctionImpl).metadata = metadata
            val errorClass = getTopLevelClass(FqName("kotlin.NotImplementedError"))
            val errorCtor = errorClass.constructors.single {
                it.valueParameters.size == 1 &&
                        it.valueParameters.single().type == context.builtIns.stringType
            }
            if (this is IrOverridableDeclaration<*>) {
                fn.overriddenSymbols = overriddenSymbols.map { it as IrSimpleFunctionSymbol }
            }
            fn.valueParameters = valueParameters.map { p -> p.copyTo(fn, defaultValue = null) }
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                +irThrow(
                    irCall(
                        this@TypeOfParamsTransformer.context.symbolTable.referenceConstructor(
                            errorCtor
                        ),
                        this@TypeOfParamsTransformer.context.typeTranslator.translateType(errorCtor.returnType)
                    ).apply {
                        putValueArgument(
                            0,
                            irString("Must be compiled with injekt compiler.")
                        )
                    }
                )
            }
        }
    }

    private fun IrFunction.copyWithTypeParams(): IrFunction {
        return copy().also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn] = fn

            // The overridden symbols might also be composable functions, so we want to make sure
            // and transform them as well
            if (this is IrOverridableDeclaration<*>) {
                fn.overriddenSymbols = overriddenSymbols.map {
                    it as IrSimpleFunctionSymbol
                    val owner = it.owner
                    val newOwner = owner.withTypeParamIfNeeded()
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }

            // if we are transforming a composable property, the jvm signature of the
            // corresponding getters and setters have a composer parameter. Since Kotlin uses the
            // lack of a parameter to determine if it is a getter, this breaks inlining for
            // composable property getters since it ends up looking for the wrong jvmSignature.
            // In this case, we manually add the appropriate "@JvmName" annotation so that the
            // inliner doesn't get confused.
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations = fn.annotations + jvmNameAnnotation(name)
            }

            // same thing for the setter
            if (descriptor is PropertySetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations = fn.annotations + jvmNameAnnotation(name)
            }

            val valueParametersMapping = explicitParameters
                .zip(fn.explicitParameters)
                .toMap()

            val usedTypeParameters = mutableSetOf<IrTypeParameter>()

            body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.symbol.descriptor.annotations.hasAnnotation(InjektClassNames.Typed) ||
                        (symbol.isBound && symbol.owner.annotations.hasAnnotation(InjektClassNames.Typed))) {
                        usedTypeParameters += (0 until expression.typeArgumentsCount)
                            .map { expression.getTypeArgument(it)!! }
                            .mapNotNull { exprTypeArg ->
                                typeParameters.firstOrNull { fnTypeParameter ->
                                    exprTypeArg == fnTypeParameter.defaultType
                                }
                            }
                    }
                    return super.visitCall(expression)
                }
            })

            val typeParams = usedTypeParameters.map {
                fn.addValueParameter(
                    "${it.name.asString()}Type",
                    context.typeTranslator.translateType(
                        KotlinTypeFactory.simpleType(
                            getTopLevelClass(InjektClassNames.Type).defaultType,
                            arguments = listOf(it.defaultType.toKotlinType().asTypeProjection())
                        )
                    )
                )
            }

            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrGetValue {
                    val newParam = valueParametersMapping[expression.symbol.owner]
                    return if (newParam != null) {
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            newParam.symbol,
                            expression.origin
                        )
                    } else expression
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    if (expression.returnTargetSymbol == oldFn.symbol) {
                        // update the return statement to point to the new function, or else
                        // it will be interpreted as a non-local return
                        return super.visitReturn(IrReturnImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            fn.symbol,
                            expression.value
                        ))
                    }
                    return super.visitReturn(expression)
                }
            })
        }
    }

    private fun IrCall.withTypeParamsIfNeeded(composerParam: IrValueParameter): IrCall {
        if (!symbol.descriptor.annotations.hasAnnotation(InjektClassNames.Typed) &&
            (symbol.isBound && symbol.owner.annotations.hasAnnotation(InjektClassNames.Typed))) return this
        val ownerFn = (symbol.owner as IrSimpleFunction).withTypeParamIfNeeded()
        if (!transformedFunctionSet.contains(ownerFn))
            return this
        if (symbol.owner == ownerFn)
            return this
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol,
            typeArgumentsCount,
            valueArgumentsCount + 1, // +1 for the composer param
            origin,
            superQualifierSymbol
        ).also {
            it.copyAttributes(this)
            it.copyTypeArgumentsFrom(this)
            it.dispatchReceiver = dispatchReceiver
            it.extensionReceiver = extensionReceiver
            for (i in 0 until valueArgumentsCount) {
                it.putValueArgument(i, getValueArgument(i))
            }
            it.putValueArgument(
                valueArgumentsCount,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    composerParam.symbol
                )
            )
        }
    }

    private fun wrapDescriptor(descriptor: FunctionDescriptor): WrappedSimpleFunctionDescriptor {
        return when (descriptor) {
            is PropertyGetterDescriptor ->
                WrappedPropertyGetterDescriptor(
                    descriptor.annotations,
                    descriptor.source
                )
            is PropertySetterDescriptor ->
                WrappedPropertySetterDescriptor(
                    descriptor.annotations,
                    descriptor.source
                )
            is DescriptorWithContainerSource ->
                WrappedFunctionDescriptorWithContainerSource(descriptor.containerSource)
            else ->
                WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)
        }
    }

    private fun IrFunction.copy(
        isInline: Boolean = this.isInline,
        modality: Modality = descriptor.modality
    ): IrSimpleFunction {
        // TODO(lmr): use deepCopy instead?
        val descriptor = descriptor
        val newDescriptor = wrapDescriptor(descriptor)

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            IrSimpleFunctionSymbolImpl(newDescriptor),
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            descriptor.isTailrec,
            descriptor.isSuspend,
            descriptor.isOperator,
            descriptor.isExpect,
            false
        ).also { fn ->
            newDescriptor.bind(fn)
            if (this is IrSimpleFunction) {
                fn.correspondingPropertySymbol = correspondingPropertySymbol
            }
            fn.parent = parent
            fn.copyTypeParametersFrom(this)
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            fn.valueParameters = valueParameters.map { p ->
                p.copyTo(fn, name = dexSafeName(p.name), type = p.type)
            }
            fn.annotations = annotations.map { a -> a }
            fn.body = body?.deepCopyWithSymbols(this)
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

    private fun jvmNameAnnotation(name: String): IrConstructorCall {
        val jvmName = context.moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(DescriptorUtils.JVM_NAME)
        )!!
        return IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.typeTranslator.translateType(jvmName.defaultType),
            context.symbolTable.referenceConstructor(jvmName.unsubstitutedPrimaryConstructor!!),
            0, 0, 1
        ).also {
            it.putValueArgument(0, IrConstImpl.string(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                context.irBuiltIns.stringType,
                name
            ))
        }
    }

    private fun getTopLevelClass(fqName: FqName): ClassDescriptor {
        return context.moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(fqName)
        )!!
    }

    /*private fun IrFile.remapComposableTypesWithComposerParam() {
        // NOTE(lmr): this operation is somewhat fragile, and the order things are done here is
        // important.
        val originalDeclarations = declarations.toList()

        // The symbolRemapper needs to traverse everything to gather symbols, so we run this first.
        acceptVoid(symbolRemapper)

        // Now that we have all of the symbols, we can clear the existing declarations, since
        // we are going to be putting new versions of them into the file.
        declarations.clear()

        originalDeclarations.mapTo(declarations) { d ->
            val typeRemapper = TypedTypeRemapper(
                context,
                symbolRemapper,
                context.typeTranslator,
                composerTypeDescriptor
            )
            // for each declaration, we create a deepCopy transformer It is important here that we
            // use the "preserving metadata" variant since we are using this copy to *replace* the
            // originals, or else the module we would produce wouldn't have any metadata in it.
            val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
                context,
                symbolRemapper,
                typeRemapper,
                typeTranslator
            ).also { typeRemapper.deepCopy = it }
            val result = d.transform(
                transformer,
                null
            ) as IrDeclaration
            // just go through and patch all of the parents to make sure things are properly wired
            // up.
            result.patchDeclarationParents(this)
            result
        }
    }*/

}

internal val TYPE_OF_DECOY_IMPL =
    object : IrDeclarationOriginImpl("TYPE_OF_DECOY_IMPL", isSynthetic = true) {}