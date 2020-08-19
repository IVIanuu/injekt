package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.SimpleUniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

class GivenExpressions(
    private val parent: GivenExpressions?,
    private val injektContext: InjektContext,
    private val contextImpl: IrClass,
    private val declarationGraph: DeclarationGraph,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer,
    private val givensGraph: GivensGraph
) {

    private val givenExpressions = mutableMapOf<Key, ContextExpression>()
    private val uniqueChildNameProvider = SimpleUniqueNameProvider()

    fun getGivenExpression(given: Given): ContextExpression {
        givenExpressions[given.key]?.let { return it }

        val rawExpression = if (given.owner != contextImpl) {
            parent!!.getGivenExpression(given)
        } else {
            when (given) {
                is GivenChildContext -> childContextExpression(given)
                is GivenFunction -> functionExpression(given)
                is GivenInstance -> inputExpression(given)
                is GivenMap -> mapExpression(given)
                is GivenNull -> nullExpression()
                is GivenSet -> setExpression(given)
            }
        }

        val finalExpression = if (given.targetContext == null ||
            given.owner != contextImpl
        ) rawExpression else ({ c ->
            val lazy = injektContext.referenceFunctions(FqName("kotlin.lazy"))
                .single { it.owner.valueParameters.size == 1 }
                .owner

            val field = contextImpl.addField(
                given.key.type.uniqueTypeName(),
                lazy.returnType.classOrNull!!.owner.typeWith(given.key.type)
            ).apply {
                initializer = irExprBody(
                    irCall(lazy).apply {
                        putTypeArgument(0, given.key.type)
                        putValueArgument(
                            0,
                            DeclarationIrBuilder(injektContext, symbol)
                                .irLambda(
                                    injektContext.tmpFunction(0)
                                        .typeWith(given.key.type)
                                ) {
                                    rawExpression(
                                        ContextExpressionContext(
                                            injektContext,
                                            contextImpl
                                        ) {
                                            irGet(contextImpl.thisReceiver!!)
                                        }
                                    )
                                }
                        )
                    }
                )
            }

            irCall(
                lazy.returnType
                    .classOrNull!!
                    .owner
                    .properties
                    .single { it.name.asString() == "value" }
                    .getter!!
            ).apply {
                dispatchReceiver = irGetField(
                    c[contextImpl],
                    field
                )
            }
        })

        val function = buildFun {
            this.name = given.key.type.uniqueTypeName()
            returnType = given.key.type
        }.apply {
            dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
            this.parent = contextImpl
            contextImpl.addChild(this)
        }

        val expression: ContextExpression = { c ->
            irCall(function).apply {
                dispatchReceiver = c[contextImpl]
            }
        }

        givenExpressions[given.key] = expression

        function.body =
            DeclarationIrBuilder(injektContext, function.symbol).run {
                irExprBody(
                    finalExpression(
                        this,
                        ContextExpressionContext(injektContext, contextImpl) {
                            irGet(function.dispatchReceiverParameter!!)
                        }
                    )
                )
            }

        return expression
    }

    private fun childContextExpression(given: GivenChildContext): ContextExpression {
        return { c ->
            val generator = ReaderContextFactoryImplGenerator(
                injektContext = injektContext,
                name = uniqueChildNameProvider("ChildFactory".asNameId()),
                factoryInterface = given.factory,
                irParent = contextImpl,
                declarationGraph = declarationGraph,
                implicitContextParamTransformer = implicitContextParamTransformer,
                parentContext = contextImpl,
                parentGraph = givensGraph,
                parentExpressions = this@GivenExpressions
            )

            val childFactory = generator.generateFactory()
            contextImpl.addChild(childFactory)

            irCall(childFactory.constructors.single()).apply {
                putValueArgument(0, c[contextImpl])
            }
        }
    }

    private fun inputExpression(
        given: GivenInstance
    ): ContextExpression = { irGetField(it[contextImpl], given.inputField) }

    private fun mapExpression(given: GivenMap): ContextExpression {
        return { c ->
            irBlock {
                val tmpMap = irTemporary(
                    irCall(
                        injektContext.referenceFunctions(
                            FqName("kotlin.collections.mutableMapOf")
                        ).first { it.owner.valueParameters.isEmpty() })
                )
                val mapType = injektContext.referenceClass(
                    FqName("kotlin.collections.Map")
                )!!
                given.functions.forEach { function ->
                    +irCall(
                        tmpMap.type.classOrNull!!
                            .functions
                            .map { it.owner }
                            .single {
                                it.name.asString() == "putAll" &&
                                        it.valueParameters.singleOrNull()?.type?.classOrNull == mapType
                            }
                    ).apply {
                        dispatchReceiver = irGet(tmpMap)
                        putValueArgument(
                            0,
                            irCall(function.symbol).apply {
                                if (function.dispatchReceiverParameter != null)
                                    dispatchReceiver =
                                        irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                                putValueArgument(valueArgumentsCount - 1, c[contextImpl])
                            }
                        )
                    }
                }

                +irGet(tmpMap)
            }
        }
    }

    private fun setExpression(given: GivenSet): ContextExpression {
        return { c ->
            irBlock {
                val tmpSet = irTemporary(
                    irCall(
                        injektContext.referenceFunctions(
                            FqName("kotlin.collections.mutableSetOf")
                        ).first { it.owner.valueParameters.isEmpty() })
                )
                val collectionType = injektContext.referenceClass(
                    FqName("kotlin.collections.Collection")
                )
                given.functions.forEach { function ->
                    +irCall(
                        tmpSet.type.classOrNull!!
                            .functions
                            .map { it.owner }
                            .single {
                                it.name.asString() == "addAll" &&
                                        it.valueParameters.singleOrNull()?.type?.classOrNull == collectionType
                            }
                    ).apply {
                        dispatchReceiver = irGet(tmpSet)
                        putValueArgument(
                            0,
                            irCall(function.symbol).apply {
                                if (function.dispatchReceiverParameter != null)
                                    dispatchReceiver =
                                        irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                                putValueArgument(valueArgumentsCount - 1, c[contextImpl])
                            }
                        )
                    }
                }

                +irGet(tmpSet)
            }
        }
    }

    private fun nullExpression(): ContextExpression = { irNull() }

    private fun functionExpression(given: GivenFunction): ContextExpression {
        return { c ->
            fun createExpression(parametersMap: Map<IrValueParameter, () -> IrExpression?>): IrExpression {
                val call = if (given.function is IrConstructor) {
                    IrConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        given.function.returnType,
                        given.function.symbol,
                        given.function.constructedClass.typeParameters.size,
                        given.function.typeParameters.size,
                        given.function.valueParameters.size
                    )
                } else {
                    IrCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        given.function.returnType,
                        given.function.symbol,
                        given.function.typeParameters.size,
                        given.function.valueParameters.size
                    )
                }
                call.apply {
                    if (given.function.dispatchReceiverParameter != null) {
                        dispatchReceiver = if (given.givenSetAccessExpression != null) {
                            given.givenSetAccessExpression!!(c)
                        } else {
                            irGetObject(
                                given.function.dispatchReceiverParameter!!.type.classOrNull!!
                            )
                        }
                    }

                    parametersMap.values.forEachIndexed { index, expression ->
                        putValueArgument(
                            index,
                            expression()
                        )
                    }

                    putValueArgument(valueArgumentsCount - 1, c[contextImpl])
                }

                return call
            }

            if (given.explicitParameters.isNotEmpty()) {
                irLambda(given.key.type) { function ->
                    val parametersMap = given.explicitParameters
                        .associateWith { parameter ->
                            {
                                irGet(
                                    function.valueParameters[parameter.index]
                                )
                            }
                        }

                    createExpression(parametersMap)
                }
            } else {
                createExpression(emptyMap())
            }
        }
    }

}

class ContextExpressionContext(
    private val expressionsByContext: Map<IrClass, () -> IrExpression>
) {

    operator fun get(context: IrClass) = expressionsByContext[context]!!()

    companion object {
        operator fun invoke(
            injektContext: InjektContext,
            thisContext: IrClass,
            thisExpression: () -> IrExpression
        ): ContextExpressionContext {
            val expressionsByContext =
                mutableMapOf<IrClass, () -> IrExpression>()
            var current: Pair<IrClass, () -> IrExpression>? = thisContext to thisExpression
            while (current != null) {
                val (currentContext, currentExpression) = current
                expressionsByContext[currentContext] = currentExpression

                val parentField = currentContext.fields
                    .singleOrNull { it.name.asString() == "parent" }
                current = if (parentField != null) {
                    val parentClass = parentField.type.classOrNull!!.owner
                    parentClass to {
                        DeclarationIrBuilder(injektContext, currentContext.symbol)
                            .irGetField(currentExpression(), parentField)
                    }
                } else {
                    null
                }
            }

            return ContextExpressionContext(expressionsByContext)
        }
    }
}

typealias ContextExpression = IrBuilderWithScope.(ContextExpressionContext) -> IrExpression
