package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.irClassReference
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.implicit.isContextSubType
import com.ivianuu.injekt.compiler.transform.implicit.isNotTransformedReaderContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addIfNotNull

class ContextTrackingTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {
    private val newIndexBuilders = mutableListOf<NewIndexBuilder>()

    private data class NewIndexBuilder(
        val path: List<String>,
        val originatingDeclaration: IrDeclarationWithName,
        val classBuilder: IrClass.() -> Unit
    )

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
                val defaultValue = declaration.defaultValue
                if (defaultValue != null && defaultValue.expression.type.isContextSubType()) {
                    newIndexBuilders += defaultValue.expression
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                declaration.type.classOrNull!!.owner,
                                subContext
                            )
                        }
                }
                return super.visitValueParameterNew(declaration)
            }

            override fun visitFieldNew(declaration: IrField): IrStatement {
                val initializer = declaration.initializer
                if (initializer != null && initializer.expression.type.isContextSubType()) {
                    newIndexBuilders += initializer.expression
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                declaration.type.classOrNull!!.owner,
                                subContext
                            )
                        }
                }
                return super.visitFieldNew(declaration)
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                val initializer = declaration.initializer
                if (initializer != null && initializer.type.isContextSubType()) {
                    newIndexBuilders += initializer
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                declaration.type.classOrNull!!.owner,
                                subContext
                            )
                        }
                }
                return super.visitVariable(declaration)
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                if (expression.symbol.owner.type.isContextSubType()) {
                    newIndexBuilders += expression.value
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                expression.symbol.owner.type.classOrNull!!.owner,
                                subContext
                            )
                        }
                }
                return super.visitSetField(expression)
            }

            override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                if (expression.symbol.owner.type.isContextSubType()) {
                    newIndexBuilders += expression.value
                        .collectContextsInExpression()
                        .map { subContext ->
                            contextImplIndexBuilder(
                                expression.symbol.owner.type.classOrNull!!.owner,
                                subContext
                            )
                        }
                }
                return super.visitSetVariable(expression)
            }

            override fun visitWhen(expression: IrWhen): IrExpression {
                val result = super.visitWhen(expression) as IrWhen
                if (expression.type.isContextSubType()) {
                    newIndexBuilders += expression.branches
                        .flatMapFix { it.result.collectContextsInExpression() }
                        .map { subContext ->
                            contextImplIndexBuilder(
                                expression.type.classOrNull!!.owner,
                                subContext
                            )
                        }
                }
                return result
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.returnType.isContextSubType()) {
                    val lastBodyStatement =
                        declaration.body?.statements?.lastOrNull() as? IrExpression
                    if (lastBodyStatement != null && lastBodyStatement.type.isContextSubType()) {
                        newIndexBuilders += lastBodyStatement
                            .collectContextsInExpression()
                            .map { subContext ->
                                contextImplIndexBuilder(
                                    declaration.returnType.classOrNull!!.owner!!,
                                    subContext
                                )
                            }
                    }

                    if (declaration is IrSimpleFunction) {
                        val field = declaration.correspondingPropertySymbol?.owner?.backingField
                        if (field != null && field.type.isContextSubType()) {
                            newIndexBuilders += contextImplIndexBuilder(
                                declaration.returnType.classOrNull!!.owner,
                                field.type.classOrNull!!.owner
                            )
                        }
                    }
                }

                return super.visitFunctionNew(declaration)
            }
        })

        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                newIndexBuilders += (0 until expression.valueArgumentsCount)
                    .mapNotNull { index ->
                        expression.getValueArgument(index)
                            ?.let { index to it }
                    }
                    .map { expression.symbol.owner.valueParameters[it.first] to it.second }
                    .filter { it.first.type.isContextSubType() }
                    .flatMapFix { (parameter, argument) ->
                        argument.collectContextsInExpression()
                            .map { context -> context to parameter.type.classOrNull!!.owner }
                    }
                    .map { (superContext, subContext) ->
                        contextImplIndexBuilder(
                            superContext,
                            subContext
                        )
                    }

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
            subContext.descriptor.fqNameSafe.asString()
        ),
        superContext
    ) {
        annotations += DeclarationIrBuilder(injektContext, subContext.symbol).run {
            irCall(injektContext.injektSymbols.contextImpl.constructors.single()).apply {
                putValueArgument(
                    0,
                    irClassReference(superContext)
                )
            }
        }
    }

    private fun IrExpression.collectContextsInExpression(): Set<IrClass> {
        val contexts = mutableSetOf<IrClass>()

        if (type.isContextSubType()) {
            contexts.addIfNotNull(type.classOrNull!!.owner)
        }

        when (this) {
            is IrGetField -> {
                if (symbol.owner.type.isContextSubType()) {
                    contexts.addIfNotNull(symbol.owner.type.classOrNull!!.owner)
                }
            }
            is IrGetValue -> {
                if (symbol.owner.type.isContextSubType()) {
                    contexts.addIfNotNull(symbol.owner.type.classOrNull!!.owner)
                }
            }
            is IrCall -> {
                if (symbol.owner.returnType.isNotTransformedReaderContext())
                    contexts.addIfNotNull(symbol.owner.returnType.classOrNull!!.owner)
            }
        }

        return contexts
    }

}