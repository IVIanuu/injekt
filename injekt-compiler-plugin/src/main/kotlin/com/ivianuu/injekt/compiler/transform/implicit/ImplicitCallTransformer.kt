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

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.isReaderLambdaInvoke
import com.ivianuu.injekt.compiler.readableName
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.remapTypeParametersByName
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ImplicitCallTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedDeclarations = mutableListOf<IrDeclaration>()

    inner class ReaderScope(
        val declaration: IrDeclaration,
        val context: IrClass
    ) {

        private val functionsByType = mutableMapOf<IrType, IrFunction>()

        private val parentFunction =
            if ((declaration as IrDeclarationWithVisibility).visibility == Visibilities.LOCAL && declaration.parent is IrFunction)
                declaration.parent as IrFunction else null

        fun inheritContext(type: IrType) {
            context.superTypes += type
        }

        fun inheritGenericContext(
            genericContext: IrClass,
            typeArguments: List<IrType>
        ): Map<IrFunction, IrFunction> {
            context.superTypes += genericContext.superTypes
                .map {
                    it.substitute(
                        genericContext.typeParameters
                            .map { it.symbol }
                            .zip(typeArguments)
                            .toMap()
                    )
                }
            val functionMap = mutableMapOf<IrFunction, IrFunction>()

            genericContext.functions
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .filterNot { it is IrConstructor }
                .forEach { genericContextFunction ->
                    val finalType =
                        genericContextFunction.returnType.substitute(
                            genericContext.typeParameters
                                .map { it.symbol }
                                .zip(typeArguments)
                                .toMap()
                        )
                            .remapTypeParametersByName(
                                declaration as IrTypeParametersContainer,
                                context
                            )
                            .let {
                                if (parentFunction != null) {
                                    it.remapTypeParametersByName(parentFunction, context)
                                } else it
                            }
                    functionMap[genericContextFunction] = context.addFunction {
                        name = finalType.readableName()
                        returnType = finalType
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = context.thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()
                    }
                }

            return functionMap
        }

        fun providerFunctionForType(
            type: IrType
        ): IrFunction {
            val finalType = type
                .remapTypeParametersByName(declaration as IrTypeParametersContainer, context)
            return functionsByType.getOrPut(finalType) {
                context.addFunction {
                    name = finalType.readableName()
                    returnType = finalType
                    modality = Modality.ABSTRACT
                }.apply {
                    dispatchReceiverParameter = context.thisReceiver?.copyTo(this)
                    addMetadataIfNotLocal()
                }
            }
        }
    }

    override fun lower() {
        module.transformChildrenVoid(
            object : IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    transformClassIfNeeded(declaration)
                    return super.visitClass(declaration)
                }

                override fun visitFunction(declaration: IrFunction): IrStatement {
                    transformFunctionIfNeeded(declaration)
                    return super.visitFunction(declaration)
                }
            }
        )
    }

    private fun transformClassIfNeeded(
        declaration: IrClass
    ) {
        if (!declaration.canUseImplicits(pluginContext)) return

        val readerConstructor = declaration.getReaderConstructor(pluginContext)!!

        val context = readerConstructor.valueParameters.single {
            it.name.asString() == "_context"
        }.type.classOrNull!!.owner

        transformDeclarationIfNeeded(
            declaration = declaration,
            declarationFunction = readerConstructor,
            context = context,
            contextExpression = { scopes ->
                if (scopes.none { it.irElement == readerConstructor }) {
                    DeclarationIrBuilder(pluginContext, declaration.symbol).run {
                        irGetField(
                            irGet(scopes.thisOfClass(declaration)!!),
                            declaration.fields.single { it.name.asString() == "_context" }
                        )
                    }
                } else {
                    DeclarationIrBuilder(pluginContext, readerConstructor.symbol)
                        .irGet(readerConstructor.valueParameters.single {
                            it.name.asString() == "_context"
                        })
                }
            }
        )
    }

    private fun transformFunctionIfNeeded(
        declaration: IrFunction
    ) {
        if (!declaration.canUseImplicits(pluginContext)) return

        transformDeclarationIfNeeded(
            declaration = declaration,
            declarationFunction = declaration,
            context = declaration.getContext()!!,
            contextExpression = {
                DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .irGet(declaration.valueParameters.single {
                        it.name.asString() == "_context"
                    })
            }
        )
    }

    private fun transformDeclarationIfNeeded(
        declaration: IrDeclarationWithName,
        declarationFunction: IrFunction,
        context: IrClass,
        contextExpression: (List<ScopeWithIr>) -> IrExpression
    ) {
        if (declaration in transformedDeclarations) return
        transformedDeclarations += declaration

        val scope = ReaderScope(declaration, context)

        declaration.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression !is IrCall &&
                    expression !is IrConstructorCall &&
                    expression !is IrDelegatingConstructorCall
                ) return expression

                if (allScopes.mapNotNull { it.irElement as? IrDeclarationWithName }
                        .filter { it.canUseImplicits(pluginContext) }
                        .last()
                        .let {
                            it != declaration && it != declarationFunction
                        }) {
                    return expression
                }

                return when {
                    expression is IrCall &&
                            expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given" ->
                        transformGivenCall(scope, expression) {
                            contextExpression(allScopes)
                        }
                    expression.isReaderLambdaInvoke(pluginContext) -> transformReaderLambdaInvoke(
                        scope,
                        expression as IrCall
                    ) {
                        contextExpression(allScopes)
                    }
                    expression.symbol.owner.canUseImplicits(pluginContext) ->
                        transformReaderCall(scope, expression) {
                            contextExpression(allScopes)
                        }
                    else -> expression
                }
            }
        }, null)
    }

    private fun transformGivenCall(
        scope: ReaderScope,
        call: IrCall,
        contextExpression: () -> IrExpression
    ): IrExpression {
        val arguments = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()
        val realType = when {
            arguments.isNotEmpty() -> pluginContext.tmpFunction(arguments.size)
                .typeWith(arguments.map { it.type } + call.getTypeArgument(0)!!)
            else -> call.getTypeArgument(0)!!
        }
        val providerFunction = scope.providerFunctionForType(realType)
        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            val rawExpression = irCall(providerFunction).apply {
                dispatchReceiver = contextExpression()
            }

            when {
                arguments.isNotEmpty() -> DeclarationIrBuilder(
                    pluginContext,
                    call.symbol
                ).irCall(
                    rawExpression.type.classOrNull!!
                        .owner
                        .functions
                        .first { it.name.asString() == "invoke" }
                ).apply {
                    dispatchReceiver = rawExpression
                    arguments.forEachIndexed { index, argument ->
                        putValueArgument(index, argument)
                    }
                }
                else -> rawExpression
            }

        }
    }

    private fun transformReaderLambdaInvoke(
        scope: ReaderScope,
        call: IrCall,
        contextExpression: () -> IrExpression
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            IrCallImpl(
                call.startOffset,
                call.endOffset,
                call.type,
                if (call.symbol.owner.isSuspend) {
                    pluginContext.tmpSuspendFunction(call.symbol.owner.valueParameters.size + 1)
                        .functions
                        .first { it.owner.name.asString() == "invoke" }
                } else {
                    pluginContext.tmpFunction(call.symbol.owner.valueParameters.size + 1)
                        .functions
                        .first { it.owner.name.asString() == "invoke" }
                }
            ).apply {
                copyTypeAndValueArgumentsFrom(call)
                putValueArgument(
                    valueArgumentsCount - 1,
                    contextExpression()
                )
            }
        }
    }

    private fun transformReaderCall(
        scope: ReaderScope,
        call: IrFunctionAccessExpression,
        contextExpression: () -> IrExpression
    ): IrExpression {
        val callee = call.symbol.owner
        transformFunctionIfNeeded(callee)
        val calleeContext = callee.getContext()!!

        // todo remove once kotlin compiler fixed IrConstructorCallImpl constructor
        val transformedCall = when (call) {
            is IrConstructorCall -> {
                IrConstructorCallImpl(
                    call.startOffset,
                    call.endOffset,
                    callee.returnType,
                    callee.symbol as IrConstructorSymbol,
                    call.typeArgumentsCount,
                    callee.typeParameters.size,
                    callee.valueParameters.size,
                    call.origin
                ).apply {
                    copyTypeAndValueArgumentsFrom(call)
                }
            }
            is IrDelegatingConstructorCall -> {
                IrDelegatingConstructorCallImpl(
                    call.startOffset,
                    call.endOffset,
                    call.type,
                    callee.symbol as IrConstructorSymbol,
                    call.typeArgumentsCount,
                    callee.valueParameters.size
                ).apply {
                    copyTypeAndValueArgumentsFrom(call)
                }
            }
            else -> {
                call as IrCall
                IrCallImpl(
                    call.startOffset,
                    call.endOffset,
                    callee.returnType,
                    callee.symbol,
                    call.origin,
                    call.superQualifierSymbol
                ).apply {
                    copyTypeAndValueArgumentsFrom(call)
                }
            }
        }

        val contextArgument =
            if (transformedCall.isReaderLambdaInvoke(pluginContext) || transformedCall.typeArgumentsCount == 0) {
                if (!transformedCall.isReaderLambdaInvoke(pluginContext))
                    scope.inheritContext(
                        calleeContext.defaultType
                            .typeWith(*transformedCall.typeArguments.toTypedArray())
                    )
                contextExpression()
            } else {
                val functionMap = scope.inheritGenericContext(
                    calleeContext,
                    transformedCall.typeArguments
                )
                DeclarationIrBuilder(pluginContext, transformedCall.symbol).run {
                    irBlock(origin = IrStatementOrigin.OBJECT_LITERAL) {
                        val contextImpl = buildClass {
                            name = Name.special("<context>")
                            visibility = Visibilities.LOCAL
                        }.apply clazz@{
                            parent = scope.declaration as IrDeclarationParent
                            createImplicitParameterDeclarationWithWrappedDescriptor()

                            superTypes += calleeContext.defaultType
                                .typeWith(*transformedCall.typeArguments.toTypedArray())

                            addConstructor {
                                returnType = defaultType
                                isPrimary = true
                                visibility = Visibilities.PUBLIC
                            }.apply {
                                DeclarationIrBuilder(pluginContext, symbol).run {
                                    body = irBlockBody {
                                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                                        +IrInstanceInitializerCallImpl(
                                            UNDEFINED_OFFSET,
                                            UNDEFINED_OFFSET,
                                            this@clazz.symbol,
                                            irBuiltIns.unitType
                                        )
                                    }
                                }
                            }

                            val implementedSuperTypes = mutableSetOf<IrType>()
                            val declarationNames = mutableSetOf<Name>()

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
                                    if (declaration.dispatchReceiverParameter?.type == irBuiltIns.anyType) continue
                                    if (declaration.name in declarationNames) continue
                                    declarationNames += declaration.name
                                    addFunction {
                                        name = declaration.name
                                        returnType = declaration.returnType.substitute(
                                            superClass.typeParameters
                                                .map { it.symbol }
                                                .zip(typeArguments)
                                                .toMap()
                                        )
                                    }.apply {
                                        dispatchReceiverParameter =
                                            thisReceiver!!.copyTo(this)
                                        overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                                        addMetadataIfNotLocal()
                                        body = DeclarationIrBuilder(
                                            pluginContext,
                                            symbol
                                        ).irExprBody(
                                            irCall(
                                                functionMap.toList().firstOrNull { (a, b) ->
                                                    a == declaration &&
                                                            a.returnType.substitute(
                                                                superClass.typeParameters
                                                                    .map { it.symbol }
                                                                    .zip(typeArguments)
                                                                    .toMap()
                                                            ) == b.returnType
                                                }?.second?.symbol ?: declaration.symbol
                                            ).apply {
                                                dispatchReceiver = contextExpression()
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
                                    superType.classOrNull!!.owner,
                                    superType.typeArguments.map { it.typeOrFail })
                            }
                        }
                        +contextImpl
                        +irCall(contextImpl.constructors.single())
                    }
                }
            }

        transformedCall.putValueArgument(transformedCall.valueArgumentsCount - 1, contextArgument)

        recordLookup(
            scope.declaration as IrDeclarationWithName,
            calleeContext
        )

        return transformedCall
    }

}
