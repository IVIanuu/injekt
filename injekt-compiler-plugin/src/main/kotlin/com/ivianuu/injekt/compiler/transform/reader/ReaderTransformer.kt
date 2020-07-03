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

package com.ivianuu.injekt.compiler.transform.reader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractFunctionTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderTransformer(
    pluginContext: IrPluginContext,
    val symbolRemapper: DeepCopySymbolRemapper
) : AbstractFunctionTransformer(pluginContext) {

    private val contextNameProvider = NameProvider()

    fun getContextForFunction(reader: IrFunction): IrClass =
        transformFunctionIfNeeded(reader).valueParameters.last().type.classOrNull!!.owner

    fun getTransformedFunction(function: IrFunction): IrFunction =
        transformFunctionIfNeeded(function)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)

        declaration.acceptVoid(symbolRemapper)

        val typeRemapper = ReaderTypeRemapper(pluginContext, symbolRemapper)
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            pluginContext,
            symbolRemapper,
            typeRemapper
        )
        declaration.files.forEach {
            it.transformChildren(
                transformer,
                null
            )
        }
        declaration.patchDeclarationParents()

        addContextClassesToFiles()

        return declaration
    }

    fun addContextClassesToFiles() {
        transformedFunctions
            .filterNot { it.key.isExternalDeclaration() }
            .forEach {
                val contextClass = it.value.valueParameters.last().type.classOrNull!!.owner
                if (contextClass !in it.value.file.declarations) {
                    it.value.file.addChild(contextClass)
                }
            }
    }

    override fun needsTransform(function: IrFunction): Boolean =
        function.isReader(pluginContext.bindingContext)

    override fun transform(function: IrFunction, callback: (IrFunction) -> Unit) {
        val transformedFunction = function.copy(pluginContext).apply {
            callback(this)

            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol).jvmNameAnnotation(
                    name
                )
                correspondingPropertySymbol?.owner?.getter = this
            }

            // same thing for the setter
            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol).jvmNameAnnotation(
                    name
                )
                correspondingPropertySymbol?.owner?.setter = this
            }
        }

        if (function.valueParameters.any { it.name.asString() == "_context" }) {
            return
        }

        val contextClassFunctionNameProvider = NameProvider()

        val parentFunction = if (transformedFunction.visibility == Visibilities.LOCAL &&
            transformedFunction.parent is IrFunction
        ) {
            transformedFunction.parent as IrFunction
        } else null

        val contextClass = buildClass {
            kind = ClassKind.INTERFACE
            name = contextNameProvider.getContextClassNameForReaderFunction(function)
        }.apply {
            parent = function.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(transformedFunction)
            parentFunction?.let { copyTypeParametersFrom(it) }

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.astName.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(transformedFunction.descriptor.fqNameSafe.asString())
                    )
                }
            }
        }

        val getCalls = mutableListOf<IrCall>()
        val readerCalls = mutableListOf<IrCall>()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>(transformedFunction)

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (functionStack.last() == transformedFunction &&
                    (expression.symbol.owner.isReader(pluginContext.bindingContext) ||
                            expression.isReaderLambdaInvoke())
                ) {
                    if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get") {
                        getCalls += expression
                    } else {
                        readerCalls += expression
                    }
                }
                return super.visitCall(expression)
            }
        })

        val providerFunctionByGetCall = getCalls.associateWith { getCall ->
            contextClass.addFunction {
                name = contextClassFunctionNameProvider.getProvideFunctionNameForGetCall(
                    transformedFunction,
                    getCall.type
                        .remapTypeParameters(transformedFunction, contextClass)
                        .let {
                            if (parentFunction != null) {
                                it.remapTypeParameters(parentFunction, contextClass)
                            } else it
                        }
                )
                returnType = getCall.type
                    .remapTypeParameters(transformedFunction, contextClass)
                    .let {
                        if (parentFunction != null) {
                            it.remapTypeParameters(parentFunction, contextClass)
                        } else it
                    }
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
                addMetadataIfNotLocal()
            }
        }

        val genericFunctionMap = mutableListOf<Pair<IrFunction, IrFunction>>()

        fun addFunctionsFromGenericContext(
            genericContext: IrClass,
            typeArguments: List<IrType>
        ) {
            contextClass.superTypes += genericContext.superTypes
            genericContext.functions
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .filterNot { it is IrConstructor }
                .forEach { genericContextFunction ->
                    genericFunctionMap += genericContextFunction to contextClass.addFunction {
                        name = contextClassFunctionNameProvider.getProvideFunctionNameForGetCall(
                            transformedFunction,
                            genericContextFunction.returnType
                        )
                        returnType = genericContextFunction.returnType
                            .substituteAndKeepQualifiers(genericContext.typeParameters
                                .map { it.symbol }
                                .zip(typeArguments).toMap()
                            )
                            .remapTypeParameters(transformedFunction, contextClass)
                            .let {
                                if (parentFunction != null) {
                                    it.remapTypeParameters(parentFunction, contextClass)
                                } else it
                            }
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()
                    }
                }
        }

        fun addSubcontext(
            subcontext: IrClass,
            typeArguments: List<IrType>
        ) {
            contextClass.superTypes += subcontext.defaultType.typeWith(*typeArguments.toTypedArray())
        }

        fun handleSubcontext(
            subcontext: IrClass,
            typeArguments: List<IrType>
        ) {
            if (subcontext.typeParameters.isNotEmpty()) {
                addFunctionsFromGenericContext(subcontext, typeArguments)
            } else {
                addSubcontext(subcontext, typeArguments)
            }
        }

        readerCalls
            .forEach { call ->
                if (!call.isReaderLambdaInvoke()) {
                    val callContext = getContextForFunction(call.symbol.owner)
                    handleSubcontext(callContext, call.typeArguments)
                } else {
                    /*val transformedLambda =
                        transformFunctionIfNeeded(lambdaSource.getFunctionArgument())
                    val lambdaContext = getContextForFunction(transformedLambda)
                    handleSubcontext(lambdaContext, call, emptyList())*/

                }
                call.getReaderLambdaArguments()
                    .forEach { expr ->
                        val transformedLambda =
                            transformFunctionIfNeeded(expr.getFunctionFromArgument())
                        val lambdaContext = getContextForFunction(transformedLambda)
                        handleSubcontext(lambdaContext, emptyList())
                    }
            }

        val contextValueParameter = transformedFunction.addValueParameter(
            "_context",
            contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
        )

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression in getCalls) {
                    val providerFunction = providerFunctionByGetCall.getValue(expression)
                    DeclarationIrBuilder(pluginContext, expression.symbol).run {
                        irCall(providerFunction).apply {
                            dispatchReceiver = irGet(contextValueParameter)
                        }
                    }
                } else super.visitCall(expression)
            }
        })

        rewriteReaderCalls(transformedFunction, genericFunctionMap)
    }

    private fun IrCall.getReaderLambdaArguments(): List<IrExpression> {
        return getArgumentsWithIr()
            .filter { (valueParameter, _) ->
                (valueParameter.type.isFunction() || valueParameter.type.isSuspendFunction()) &&
                        valueParameter.type.hasAnnotation(InjektFqNames.Reader)
            }
            .map { it.second }
    }

    private fun IrExpression.getFunctionFromArgument() = when (this) {
        is IrFunctionExpression -> function
        else -> error("Cannot extract function from $this ${dumpSrc()}")
    }

    private fun rewriteReaderCalls(
        function: IrFunction,
        genericFunctionMap: List<Pair<IrFunction, IrFunction>>
    ) {
        function.rewriteTransformedFunctionRefs()

        function.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = mutableListOf(function)

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (functionStack.last() != function) return result
                if (!result.symbol.owner.isReader(pluginContext.bindingContext) &&
                    !result.isReaderLambdaInvoke()
                ) return result

                if (result.isReaderLambdaInvoke()) {
                    return DeclarationIrBuilder(pluginContext, result.symbol).run {
                        IrCallImpl(
                            result.startOffset,
                            result.endOffset,
                            result.type,
                            if (result.symbol.owner.isSuspend) {
                                pluginContext.tmpSuspendFunction(result.symbol.owner.valueParameters.size + 1)
                                    .functions
                                    .first { it.owner.name.asString() == "invoke" }
                            } else {
                                pluginContext.tmpFunction(result.symbol.owner.valueParameters.size + 1)
                                    .functions
                                    .first { it.owner.name.asString() == "invoke" }
                            }
                        ).apply {
                            copyTypeAndValueArgumentsFrom(result)
                            putValueArgument(
                                valueArgumentsCount - 1,
                                irGet(function.valueParameters.last())
                            )
                        }
                    }
                }

                val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)
                val transformedCall = transformCall(transformedCallee, result)

                val contextArgument =
                    DeclarationIrBuilder(pluginContext, transformedCall.symbol).run {
                        if (transformedCallee.typeParameters.isNotEmpty() && !transformedCall.isReaderLambdaInvoke()) {
                            val calleeContext = getContextForFunction(transformedCallee)

                            irBlock(origin = IrStatementOrigin.OBJECT_LITERAL) {
                                val contextImpl = buildClass {
                                    name = Name.special("<context>")
                                    visibility = Visibilities.LOCAL
                                }.apply clazz@{
                                    parent = function
                                    createImplicitParameterDeclarationWithWrappedDescriptor()

                                    superTypes += calleeContext.defaultType
                                        .typeWith(*transformedCall.typeArguments.toTypedArray())

                                    result.getReaderLambdaArguments()
                                        .forEach { expr ->
                                            val transformedLambda =
                                                transformFunctionIfNeeded(expr.getFunctionFromArgument())
                                            superTypes += getContextForFunction(transformedLambda)
                                                .defaultType
                                        }

                                    addConstructor {
                                        returnType = defaultType
                                        isPrimary = true
                                        visibility = Visibilities.PUBLIC
                                    }.apply {
                                        InjektDeclarationIrBuilder(pluginContext, symbol).run {
                                            body = builder.irBlockBody {
                                                initializeClassWithAnySuperClass(this@clazz.symbol)
                                            }
                                        }
                                    }

                                    val implementedSuperTypes = mutableSetOf<IrType>()

                                    fun implementFunctions(
                                        superClass: IrClass,
                                        typeArguments: List<IrType>
                                    ) {
                                        if (superClass.defaultType in implementedSuperTypes) return
                                        implementedSuperTypes += superClass.defaultType
                                        for (declaration in superClass.declarations.toList()) {
                                            if (declaration !is IrFunction) continue
                                            if (declaration is IrConstructor) continue
                                            if (declaration.isFakeOverride) continue
                                            if (declaration.dispatchReceiverParameter?.type == irBuiltIns.anyType) break
                                            addFunction {
                                                name = declaration.name
                                                returnType = declaration.returnType
                                                    .substituteAndKeepQualifiers(
                                                        superClass.typeParameters.map { it.symbol }
                                                            .associateWith {
                                                                typeArguments[it.owner.index]
                                                            }
                                                    )
                                                visibility = declaration.visibility
                                            }.apply {
                                                dispatchReceiverParameter =
                                                    thisReceiver!!.copyTo(this)
                                                addMetadataIfNotLocal()
                                                body = DeclarationIrBuilder(
                                                    pluginContext,
                                                    symbol
                                                ).irExprBody(
                                                    irCall(
                                                        genericFunctionMap.firstOrNull { (a, b) ->
                                                            a == declaration &&
                                                                    a.returnType.substituteAndKeepQualifiers(
                                                                        superClass.typeParameters
                                                                            .map { it.symbol }
                                                                            .zip(typeArguments)
                                                                            .toMap()
                                                                    ) == b.returnType
                                                        }?.second?.symbol ?: declaration.symbol
                                                    ).apply {
                                                        dispatchReceiver =
                                                            irGet(function.valueParameters.last())
                                                    }
                                                )
                                            }
                                        }

                                        superClass.superTypes
                                            .map { it to it.classOrNull?.owner }
                                            .forEach { (superType, clazz) ->
                                                if (clazz != null)
                                                    implementFunctions(
                                                        clazz,
                                                        superType.typeArguments.map { it.typeOrFail })
                                            }
                                    }

                                    superTypes.forEach { superType ->
                                        implementFunctions(
                                            superType.getClass()!!,
                                            superType.typeArguments.map { it.typeOrFail })
                                    }
                                }
                                +contextImpl
                                +irCall(contextImpl.constructors.single())
                            }
                        } else {
                            irGet(function.valueParameters.last())
                        }
                    }

                return transformedCall.apply {
                    putValueArgument(valueArgumentsCount - 1, contextArgument)
                }
            }
        })
    }

    override fun transformExternal(function: IrFunction, callback: (IrFunction) -> Unit) {
        val transformedFunction = function.copy(pluginContext)
        callback(transformedFunction)

        if (transformedFunction.valueParameters.any { it.name.asString() == "_context" }) {
            return
        }

        val contextClass =
            moduleFragment.descriptor.getPackage(function.getPackageFragment()!!.fqName)
                .memberScope
                .getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .filter { it.hasAnnotation(InjektFqNames.AstName) }
                .single {
                    it.annotations.findAnnotation(InjektFqNames.AstName)!!
                        .argumentValue("name")
                        .let { it as StringValue }
                        .value == function.descriptor.fqNameSafe.asString()
                }
                .let { pluginContext.referenceClass(it.fqNameSafe)!!.owner }

        transformedFunction.addValueParameter(
            "_context",
            contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
        )
    }

    private fun NameProvider.getContextClassNameForReaderFunction(function: IrFunction): Name {
        return allocateForGroup(
            getJoinedName(
                function.getPackageFragment()!!.fqName,
                function.descriptor.fqNameSafe
                    .parent()
                    .let {
                        if (function.name.isSpecial) {
                            it.child(allocateForGroup("Lambda") + "_Context")
                        } else {
                            it.child(function.name.asString() + "_Context")
                        }
                    }

            )
        )
    }

    private fun NameProvider.getProvideFunctionNameForGetCall(
        function: IrFunction,
        type: IrType
    ): Name {
        return allocateForGroup(
            getJoinedName(
                function.getPackageFragment()!!.fqName,
                function.descriptor.fqNameSafe
                    .parent()
                    .child(type.classifierOrFail.descriptor.name)
            )
        )
    }

}
