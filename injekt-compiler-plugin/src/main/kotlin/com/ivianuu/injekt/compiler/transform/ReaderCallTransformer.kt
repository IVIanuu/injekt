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

import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.canUseReaders
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.irClassReference
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.remapTypeParametersByName
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transformFiles
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderCallTransformer(
    pluginContext: IrPluginContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(pluginContext) {

    private val transformedDeclarations = mutableListOf<IrDeclaration>()

    override fun lower() {
        module.transformFiles(
            object : IrElementTransformerVoidWithContext() {
                override fun visitClassNew(declaration: IrClass): IrStatement {
                    transformClassIfNeeded(declaration)
                    return super.visitClassNew(declaration)
                }

                override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                    transformFunctionIfNeeded(declaration)
                    return super.visitFunctionNew(declaration)
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val result = super.visitCall(expression) as IrCall
                    return when {
                        expression.symbol.descriptor.fqNameSafe.asString() ==
                                "com.ivianuu.injekt.rootContext" -> {
                            transformContextCall(
                                null,
                                result,
                                currentFile,
                                null
                            )
                        }
                        expression.symbol.descriptor.fqNameSafe.asString() ==
                                "com.ivianuu.injekt.runReader" -> {
                            transformRunReaderCall(
                                null,
                                currentScope!!.irElement as IrDeclaration,
                                result,
                                null
                            )
                        }
                        else -> {
                            result
                        }
                    }
                }
            }
        )
    }

    inner class ReaderScope(
        val declaration: IrDeclaration,
        val context: IrClass
    ) {

        private val functionsByType = mutableMapOf<IrType, IrFunction>()

        private val parentFunction =
            if ((declaration as IrDeclarationWithVisibility).visibility == Visibilities.LOCAL && declaration.parent is IrFunction)
                declaration.parent as IrFunction else null

        fun givenExpressionForType(
            type: IrType,
            contextExpression: () -> IrExpression
        ): IrExpression {
            val finalType = type
                .remapTypeParametersByName(declaration as IrTypeParametersContainer, context)
                .let {
                    if (parentFunction != null)
                        it.remapTypeParametersByName(parentFunction, context)
                    else it
                }

            val function = functionsByType.getOrPut(finalType) {
                context.addFunction {
                    name = finalType.uniqueTypeName()
                    returnType = finalType
                    modality = Modality.ABSTRACT
                }.apply {
                    dispatchReceiverParameter = context.thisReceiver?.copyTo(this)
                    addMetadataIfNotLocal()
                    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                        irCall(injektSymbols.origin.constructors.single()).apply {
                            putValueArgument(
                                0,
                                irString(declaration.descriptor.fqNameSafe.asString())
                            )
                        }
                    }
                }
            }

            return DeclarationIrBuilder(pluginContext, function.symbol).run {
                irCall(function).apply {
                    dispatchReceiver = contextExpression()
                }
            }
        }
    }

    private fun transformClassIfNeeded(
        declaration: IrClass
    ) {
        if (!declaration.canUseReaders(pluginContext)) return

        val readerConstructor = declaration.getReaderConstructor(pluginContext)!!

        val context = readerConstructor.getContext()!!

        transformDeclarationIfNeeded(
            declaration = declaration,
            declarationFunction = readerConstructor,
            context = context,
            contextExpression = { scopes ->
                if (scopes.none { it.irElement == readerConstructor }) {
                    DeclarationIrBuilder(this.pluginContext, declaration.symbol).run {
                        irGetField(
                            irGet(scopes.thisOfClass(declaration)!!),
                            declaration.fields.single { it.name.asString() == "_context" }
                        )
                    }
                } else {
                    DeclarationIrBuilder(this.pluginContext, readerConstructor.symbol)
                        .irGet(readerConstructor.getContextValueParameter()!!)
                }
            }
        )
    }

    private fun transformFunctionIfNeeded(
        declaration: IrFunction
    ) {
        if (!declaration.canUseReaders(pluginContext)) return

        transformDeclarationIfNeeded(
            declaration = declaration,
            declarationFunction = declaration,
            context = declaration.getContext() ?: error("Wtf ${declaration.dump()}"),
            contextExpression = {
                DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .irGet(declaration.getContextValueParameter()!!)
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

                if (allScopes
                        .mapNotNull { it.irElement as? IrDeclarationWithName }
                        .last { it.canUseReaders(pluginContext) }
                        .let {
                            it != declaration && it != declarationFunction
                        }
                ) {
                    return expression
                }

                return when {
                    expression is IrCall &&
                            expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given" ->
                        transformGivenCall(scope, expression) {
                            contextExpression(allScopes)
                        }
                    expression is IrCall &&
                            (expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.rootContext" ||
                                    expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.childContext") ->
                        transformContextCall(scope, expression, scope.declaration.file) {
                            contextExpression(allScopes)
                        }
                    expression is IrCall &&
                            expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader" ->
                        transformRunReaderCall(scope, null, expression) {
                            contextExpression(allScopes)
                        }
                    expression.symbol.owner.canUseReaders(pluginContext) ->
                        transformReaderCall(scope, expression) {
                            contextExpression(allScopes)
                        }
                    else -> expression
                }
            }
        }, null)
    }

    private fun transformContextCall(
        scope: ReaderScope?,
        call: IrCall,
        file: IrFile,
        contextExpression: (() -> IrExpression)?
    ): IrExpression {
        val inputs = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        val contextType = call.getTypeArgument(0)!!

        val isChild = call.symbol.descriptor.fqNameSafe.asString() ==
                "com.ivianuu.injekt.childContext"

        val contextFactory = createContextFactory(
            contextType = contextType,
            file = file,
            startOffset = call.startOffset,
            inputTypes = inputs.map { it.type },
            pluginContext = pluginContext,
            isChild = isChild,
            typeParametersContainer = scope?.declaration as? IrTypeParametersContainer,
            module = module,
            injektSymbols = injektSymbols
        ).also {
            if (!isChild) {
                indexer.index(
                    listOf(DeclarationGraph.ROOT_CONTEXT_FACTORY_PATH),
                    it
                )
            }
        }

        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            irCall(contextFactory.functions.single()).apply {
                dispatchReceiver = if (isChild) {
                    scope!!.givenExpressionForType(
                        if (scope.declaration is IrTypeParametersContainer)
                            contextFactory.typeWith(
                                scope.declaration.typeParameters
                                    .map { it.defaultType }
                            )
                        else contextFactory.defaultType,
                        contextExpression!!
                    )
                } else {
                    val contextFactoryImplStub = buildClass {
                        this.name = (contextFactory.name.asString() + "Impl").asNameId()
                        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                        kind = ClassKind.OBJECT
                        visibility = Visibilities.INTERNAL
                    }.apply clazz@{
                        createImplicitParameterDeclarationWithWrappedDescriptor()
                        parent = IrExternalPackageFragmentImpl(
                            IrExternalPackageFragmentSymbolImpl(
                                EmptyPackageFragmentDescriptor(
                                    pluginContext.moduleDescriptor,
                                    file.fqName
                                )
                            ),
                            file.fqName
                        )
                    }
                    irGetObject(contextFactoryImplStub.symbol)
                }

                inputs.forEachIndexed { index, input ->
                    putValueArgument(index, input)
                }
            }
        }
    }

    private fun transformRunReaderCall(
        scope: ReaderScope?,
        irScope: IrDeclaration?,
        call: IrCall,
        contextExpression: (() -> IrExpression)?
    ): IrExpression {
        val runReaderCallContextExpression = call.extensionReceiver!!
        val lambdaExpression = call.getValueArgument(0)!! as IrFunctionExpression

        val scopeDeclaration = (scope?.declaration ?: irScope!!)
        recordLookup(scopeDeclaration, runReaderCallContextExpression.type.classOrNull!!.owner)

        indexer.index(
            runReaderCallContextExpression.type.classOrNull!!.owner,
            (scopeDeclaration
                .descriptor.fqNameSafe.asString().hashCode() + call.startOffset)
                .toString()
                .removeIllegalChars()
                .asNameId(),
            listOf(
                DeclarationGraph.RUN_READER_CALL_PATH,
                runReaderCallContextExpression.type.classOrNull!!.owner.descriptor.fqNameSafe.asString()
            )
        ) {
            recordLookup(this, scopeDeclaration as IrDeclarationWithName)
            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(injektSymbols.runReaderCall.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irClassReference(lambdaExpression.function.getContext()!!)
                    )
                }
            }
        }

        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            irCall(
                pluginContext.referenceFunctions(
                    FqName("com.ivianuu.injekt.internal.runReaderDummy")
                ).single()
            ).apply {
                putTypeArgument(0, runReaderCallContextExpression.type)
                putTypeArgument(1, lambdaExpression.type.typeArguments.last().typeOrFail)
                putValueArgument(0, runReaderCallContextExpression)
                putValueArgument(1, lambdaExpression)
            }
        }
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
                .owner
                .typeWith(arguments.map { it.type } + call.getTypeArgument(0)!!)
            else -> call.getTypeArgument(0)!!
        }
        val rawExpression = scope.givenExpressionForType(realType, contextExpression)
        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            when {
                arguments.isNotEmpty() -> DeclarationIrBuilder(
                    context,
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

        val contextArgument = scope.givenExpressionForType(
            transformedCall.symbol.owner
                .getContext()!!
                .typeWith(transformedCall.typeArguments),
            contextExpression
        )

        transformedCall.putValueArgument(transformedCall.valueArgumentsCount - 1, contextArgument)

        return transformedCall
    }

}
