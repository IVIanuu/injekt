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

import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.irClassReference
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import com.ivianuu.injekt.compiler.isReaderLambdaInvoke
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addIfNotNull

class ReaderTrackingTransformer(
    pluginContext: IrPluginContext,
    private val indexer: Indexer,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()
    private val newDeclarations = mutableListOf<IrDeclaration>()

    private sealed class Scope {
        abstract val file: IrFile
        abstract val fqName: FqName
        abstract val invocationContext: IrClass

        class Reader(
            val declaration: IrDeclaration,
            override val invocationContext: IrClass
        ) : Scope() {
            override val file: IrFile
                get() = declaration.file
            override val fqName: FqName
                get() = declaration.descriptor.fqNameSafe
        }

        class RunReader(
            val call: IrCall,
            override val file: IrFile,
            override val fqName: FqName
        ) : Scope() {

            override val invocationContext =
                (call.getValueArgument(1) as IrFunctionExpression)
                    .function
                    .getContext()!!

            fun isBlock(function: IrFunction): Boolean =
                call.getValueArgument(0).let {
                    it is IrFunctionExpression &&
                            it.function == function
                }
        }
    }

    private var currentReaderScope: Scope? = null

    private inline fun <R> inScope(scope: Scope, block: () -> R): R {
        val previousScope = currentReaderScope
        currentReaderScope = scope
        val result = block()
        currentReaderScope = previousScope
        return result
    }

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
                val defaultValue = declaration.defaultValue
                if (defaultValue != null && defaultValue.expression.type.isTransformedReaderLambda()) {
                    newDeclarations += defaultValue.expression
                        .collectReaderLambdaContextsInExpression()
                        .map { subContext ->
                            readerImplIndex(
                                currentScope!!.scope.scopeOwner.fqNameSafe,
                                currentFile,
                                declaration.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitValueParameterNew(declaration)
            }

            override fun visitFieldNew(declaration: IrField): IrStatement {
                val initializer = declaration.initializer
                if (initializer != null && initializer.expression.type.isTransformedReaderLambda()) {
                    newDeclarations += initializer.expression
                        .collectReaderLambdaContextsInExpression()
                        .map { subContext ->
                            readerImplIndex(
                                currentScope!!.scope.scopeOwner.fqNameSafe,
                                currentFile,
                                declaration.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitFieldNew(declaration)
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                if (expression.symbol.owner.type.isTransformedReaderLambda()) {
                    newDeclarations += expression.value
                        .collectReaderLambdaContextsInExpression()
                        .map { subContext ->
                            readerImplIndex(
                                currentScope!!.scope.scopeOwner.fqNameSafe,
                                currentFile,
                                expression.symbol.owner.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitSetField(expression)
            }

            override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                if (expression.symbol.owner.type.isTransformedReaderLambda()) {
                    newDeclarations += expression.value
                        .collectReaderLambdaContextsInExpression()
                        .map { subContext ->
                            readerImplIndex(
                                currentScope!!.scope.scopeOwner.fqNameSafe,
                                currentFile,
                                expression.symbol.owner.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitSetVariable(expression)
            }

            override fun visitClassNew(declaration: IrClass): IrStatement {
                return if (declaration.canUseImplicits(pluginContext)) {
                    inScope(
                        Scope.Reader(
                            declaration,
                            declaration.getReaderConstructor(pluginContext)!!
                                .getContext()!!
                        )
                    ) {
                        super.visitClassNew(declaration)
                    }
                } else super.visitClassNew(declaration)
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration is IrSimpleFunction &&
                    declaration.returnType.isTransformedReaderLambda()
                ) {
                    val field = declaration.correspondingPropertySymbol?.owner?.backingField
                    if (field != null && field.type.isTransformedReaderLambda()) {
                        newDeclarations += readerImplIndex(
                            currentScope!!.scope.scopeOwner.fqNameSafe,
                            currentFile,
                            declaration.returnType.lambdaContext!!,
                            field.type.lambdaContext!!
                        )
                    }
                }

                return if (declaration.canUseImplicits(pluginContext) &&
                    currentReaderScope.let {
                        it == null || it !is Scope.RunReader ||
                                !it.isBlock(declaration)
                    }
                ) {
                    inScope(
                        Scope.Reader(
                            declaration,
                            declaration.getContext()!!
                        )
                    ) {
                        super.visitFunctionNew(declaration)
                    }
                } else super.visitFunctionNew(declaration)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.runReader"
                ) {
                    currentScope!!.scope.scopeOwner.fqNameSafe
                    inScope(
                        Scope.RunReader(
                            expression,
                            currentFile,
                            currentScope!!.scope.scopeOwner.fqNameSafe
                        )
                    ) {
                        super.visitCall(expression)
                    }
                } else {
                    if (expression.isReaderLambdaInvoke(pluginContext)) {
                        visitReaderLambdaInvoke(expression)
                    }
                    super.visitCall(expression)
                }
            }
        })

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.runReader"
                ) return super.visitFunctionAccess(expression)

                val transformedCallee = implicitContextParamTransformer
                    .getTransformedFunction(expression.symbol.owner)

                newDeclarations += (0 until expression.valueArgumentsCount)
                    .mapNotNull { index ->
                        expression.getValueArgument(index)
                            ?.let { index to it }
                    }
                    .map { transformedCallee.valueParameters[it.first] to it.second }
                    .filter { it.first.type.isTransformedReaderLambda() }
                    .flatMapFix { (parameter, argument) ->
                        argument.collectReaderLambdaContextsInExpression()
                            .map { context ->
                                (parameter.type.lambdaContext
                                    ?: error("null for ${parameter.dump()}\n${expression.symbol.owner.dump()}")) to context
                            }
                    }
                    .map { (superContext, subContext) ->
                        readerImplIndex(
                            currentScope!!.scope.scopeOwner.fqNameSafe,
                            currentFile,
                            superContext,
                            subContext
                        )
                    }

                return super.visitFunctionAccess(expression)
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration is IrSimpleFunction &&
                    declaration.isMarkedAsImplicit(pluginContext) &&
                    declaration.overriddenSymbols.isNotEmpty()
                ) {
                    newDeclarations += readerImplIndex(
                        declaration.descriptor.fqNameSafe,
                        currentFile,
                        declaration.overriddenSymbols
                            .single()
                            .owner
                            .getContext()!!,
                        declaration
                            .getContext()!!
                    )
                }

                return super.visitFunctionNew(declaration)
            }

        })

        newDeclarations.forEach {
            it.file.addChild(it)
            indexer.index(it as IrDeclarationWithName)
        }
    }

    private fun visitReaderLambdaInvoke(call: IrCall) {
        val scope = currentReaderScope!!

        val lambdaContext = try {
            call.dispatchReceiver!!.type.lambdaContext!!
        } catch (e: Exception) {
            error("Failed to get context from lambda ${call.dump()}")
        }

        newDeclarations += readerInvocationIndex(
            scope.fqName,
            scope.file,
            lambdaContext,
            scope.invocationContext
        )
    }

    private fun readerInvocationIndex(
        fqName: FqName,
        file: IrFile,
        lambdaContext: IrClass,
        invocationContext: IrClass
    ) = baseIndex(
        fqName,
        "ReaderInvocation",
        file,
        DeclarationIrBuilder(pluginContext, invocationContext.symbol).run {
            irCall(symbols.readerInvocation.constructors.single()).apply {
                putValueArgument(
                    0,
                    irClassReference(lambdaContext)
                )
                putValueArgument(
                    1,
                    irClassReference(invocationContext)
                )
            }
        }
    )

    private fun readerImplIndex(
        fqName: FqName,
        file: IrFile,
        superContext: IrClass,
        subContext: IrClass
    ) = baseIndex(
        fqName,
        "ReaderImpl",
        file,
        DeclarationIrBuilder(pluginContext, subContext.symbol).run {
            irCall(symbols.readerImpl.constructors.single()).apply {
                putValueArgument(
                    0,
                    irClassReference(superContext)
                )
                putValueArgument(
                    1,
                    irClassReference(subContext)
                )
            }
        }
    )

    private fun baseIndex(
        fqName: FqName,
        nameSuffix: String,
        file: IrFile,
        annotation: IrConstructorCall
    ) = buildClass {
        name = nameProvider.allocateForGroup(
            "${fqName.pathSegments().joinToString("_")}$nameSuffix".asNameId()
        )
        kind = ClassKind.INTERFACE
        visibility = Visibilities.INTERNAL
    }.apply {
        parent = file
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()
        annotations += annotation
    }

    private fun IrExpression.collectReaderLambdaContextsInExpression(): List<IrClass> {
        val contexts = mutableListOf<IrClass>()

        when (this) {
            is IrGetField -> {
                if (symbol.owner.type.isTransformedReaderLambda()) {
                    contexts.addIfNotNull(symbol.owner.type.lambdaContext)
                }
            }
            is IrGetValue -> {
                if (symbol.owner.type.isTransformedReaderLambda()) {
                    contexts.addIfNotNull(symbol.owner.type.lambdaContext)
                }
            }
            is IrFunctionExpression -> {
                contexts.addIfNotNull(function.getContext())
            }
        }

        if (type.isTransformedReaderLambda()) {
            contexts.addIfNotNull(type.lambdaContext)
        }

        return contexts
    }

}
