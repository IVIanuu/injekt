package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext

class ContextTrackingTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {

    override fun lower() {
    }

    /*private val newIndexBuilders = mutableListOf<NewIndexBuilder>()

    private data class NewIndexBuilder(
        val path: List<String>,
        val originatingDeclaration: IrDeclarationWithName,
        val classBuilder: IrClass.() -> Unit
    )

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
                val defaultValue = declaration.defaultValue
                if (defaultValue != null && defaultValue.expression.type.isTransformedReaderContext()) {
                    newIndexBuilders += defaultValue.expression
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                declaration.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitValueParameterNew(declaration)
            }

            override fun visitFieldNew(declaration: IrField): IrStatement {
                val initializer = declaration.initializer
                if (initializer != null && initializer.expression.type.isTransformedReaderContext()) {
                    newIndexBuilders += initializer.expression
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                declaration.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitFieldNew(declaration)
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                val initializer = declaration.initializer
                if (initializer != null && initializer.type.isTransformedReaderContext()) {
                    newIndexBuilders += initializer
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                declaration.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitVariable(declaration)
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                if (expression.symbol.owner.type.isTransformedReaderContext()) {
                    newIndexBuilders += expression.value
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                expression.symbol.owner.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitSetField(expression)
            }

            override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                if (expression.symbol.owner.type.isTransformedReaderContext()) {
                    newIndexBuilders += expression.value
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                expression.symbol.owner.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return super.visitSetVariable(expression)
            }

            override fun visitWhen(expression: IrWhen): IrExpression {
                val result = super.visitWhen(expression) as IrWhen
                if (expression.type.isTransformedReaderContext()) {
                    newIndexBuilders += expression.branches
                        .flatMapFix { it.result.collectContextsInExpression() }
                        .map { subContext ->
                            contextImplIndexBuilder(
                                expression.type.lambdaContext!!,
                                subContext
                            )
                        }
                }
                return result
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.returnType.isTransformedReaderContext()) {
                    val lastBodyStatement =
                        declaration.body?.statements?.lastOrNull() as? IrExpression
                    if (lastBodyStatement != null && lastBodyStatement.type.isTransformedReaderContext()) {
                        newIndexBuilders += lastBodyStatement
                            .collectContextsInExpression()
                            .map { subContext ->
                                contextImplIndexBuilder(
                                    declaration.returnType.lambdaContext!!,
                                    subContext
                                )
                            }
                    }

                    if (declaration is IrSimpleFunction) {
                        val field = declaration.correspondingPropertySymbol?.owner?.backingField
                        if (field != null && field.type.isTransformedReaderContext()) {
                            newIndexBuilders += contextImplIndexBuilder(
                                declaration.returnType.lambdaContext!!,
                                field.type.lambdaContext!!
                            )
                        }
                    }
                }

                return super.visitFunctionNew(declaration)
            }
        })

        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                /*newIndexBuilders += (0 until expression.valueArgumentsCount)
                    .mapNotNull { index ->
                        expression.getValueArgument(index)
                            ?.let { index to it }
                    }
                    .map { transformedCallee.valueParameters[it.first] to it.second }
                    .filter { it.first.type.isReaderContext() }
                    .flatMapFix { (parameter, argument) ->
                        argument.collectReaderLambdaContextsInExpression()
                            .map { context ->
                                (parameter.type.lambdaContext
                                    ?: error("null for ${parameter.dump()}\n${expression.symbol.owner.dump()}")) to context
                            }
                    }
                    .map { (superContext, subContext) ->
                        contextImplIndexBuilder(
                            superContext,
                            subContext
                        )
                    }*/

                return super.visitFunctionAccess(expression)
            }

        })

        newIndexBuilders.forEach {
            indexer.index(
                originatingDeclaration = it.originatingDeclaration,
                path = it.path,
                classBuilder = it.classBuilder
            )
        }
    }

    private fun contextImplIndexBuilder(
        superContext: IrClass,
        subContext: IrClass
    ) = NewIndexBuilder(
        listOf(
            DeclarationGraph.CONTEXT_IMPL_PATH,
            superContext.descriptor.fqNameSafe.asString()
        ),
        subContext
    ) {
        annotations += DeclarationIrBuilder(injektContext, subContext.symbol).run {
            irCall(injektContext.injektSymbols.contextImpl.constructors.single()).apply {
                putValueArgument(
                    0,
                    irClassReference(subContext)
                )
            }
        }
    }

    private fun IrExpression.collectContextsInExpression(): Set<IrClass> {
        val contexts = mutableSetOf<IrClass>()

        if (type.isTransformedReaderContext()) {
            contexts.addIfNotNull(type.lambdaContext)
        }

        when (this) {
            is IrGetField -> {
                if (symbol.owner.type.isTransformedReaderContext()) {
                    contexts.addIfNotNull(symbol.owner.type.lambdaContext)
                }
            }
            is IrGetValue -> {
                if (symbol.owner.type.isTransformedReaderContext()) {
                    contexts.addIfNotNull(symbol.owner.type.lambdaContext)
                }
            }
            is IrFunctionExpression -> {
                contexts.addIfNotNull(function.getContext())
            }
            is IrCall -> {
                contexts.addIfNotNull(symbol.owner.getContext())
            }
        }

        return contexts
    }*/

}