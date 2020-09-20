package com.ivianuu.injekt.compiler.backend.readercontextimpl

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.backend.irBuilderTmp
import com.ivianuu.injekt.compiler.backend.irLambda
import com.ivianuu.injekt.compiler.backend.pluginContext
import com.ivianuu.injekt.compiler.backend.tmpFunction
import com.ivianuu.injekt.compiler.backend.uniqueTypeName
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irSetVar
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName

@com.ivianuu.injekt.Given
class GivenExpressions(
    private val parent: GivenExpressions?,
    private val contextImpl: IrClass
) {

    private val givenExpressions = mutableMapOf<Key, ContextExpression>()
    val uniqueChildNameProvider = UniqueNameProvider()

    fun getGivenExpression(
        given: Given,
        superFunction: IrFunction?
    ): ContextExpression {
        givenExpressions[given.key]?.let { return it }

        val rawExpression = if (given.owner != contextImpl) {
            parent!!.getGivenExpression(given, null)
        } else {
            when (given) {
                is GivenCalleeContext -> calleeContextExpression(given)
                is GivenChildContext -> childContextExpression(given)
                is GivenFunction -> functionExpression(given)
                is GivenInstance -> inputExpression(given)
                is GivenMap -> mapExpression(given)
                is GivenNull -> nullExpression()
                is GivenSelfContext -> selfContextExpression(given)
                is GivenSet -> setExpression(given)
            }
        }

        val functionByType = buildFun {
            this.name = given.key.type.uniqueTypeName()
            returnType = given.key.type
        }.apply {
            dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
            this.parent = contextImpl
            contextImpl.addChild(this)
            if (superFunction is IrSimpleFunction && name == superFunction.name) {
                overriddenSymbols += superFunction.symbol
            }
        }

        val finalExpression = if (given.targetContext == null ||
            given.owner != contextImpl
        ) rawExpression else ({ c ->
            val field = contextImpl.addField(
                given.key.type.uniqueTypeName(),
                pluginContext.irBuiltIns.anyNType
            ).apply {
                initializer = irExprBody(irGet(contextImpl.thisReceiver!!))
            }

            irBlock(resultType = given.key.type) {
                val tmp1 = irTemporary(irGetField(c[contextImpl], field))
                +irIfThenElse(
                    pluginContext.irBuiltIns.unitType,
                    irNot(irEqeqeq(irGet(tmp1), c[contextImpl])),
                    IrReturnImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        given.key.type,
                        functionByType.symbol,
                        irGet(tmp1)
                    ),
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.synchronized"))
                            .single()
                    ).apply {
                        putTypeArgument(0, given.key.type)
                        putValueArgument(0, c[contextImpl])
                        putValueArgument(
                            1,
                            irLambda(pluginContext.tmpFunction(0).typeWith(given.key.type)) {
                                irBlock {
                                    val tmp2 = irTemporary(irGetField(c[contextImpl], field))
                                    +irIfThenElse(
                                        pluginContext.irBuiltIns.unitType,
                                        irNot(irEqeqeq(irGet(tmp2), c[contextImpl])),
                                        IrReturnImpl(
                                            UNDEFINED_OFFSET,
                                            UNDEFINED_OFFSET,
                                            given.key.type,
                                            functionByType.symbol,
                                            irGet(tmp2)
                                        ),
                                        irBlock {
                                            +irSetVar(tmp2.symbol, rawExpression(c))
                                            +irSetField(c[contextImpl], field, irGet(tmp2))
                                            +IrReturnImpl(
                                                UNDEFINED_OFFSET,
                                                UNDEFINED_OFFSET,
                                                given.key.type,
                                                functionByType.symbol,
                                                irGet(tmp2)
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        })

        val expression: ContextExpression = { c ->
            irCall(functionByType).apply {
                dispatchReceiver = c[contextImpl]
            }
        }

        givenExpressions[given.key] = expression

        functionByType.body = functionByType.irBuilderTmp().run {
            irExprBody(
                finalExpression(
                    this,
                    ContextExpressionContext(contextImpl) {
                        irGet(functionByType.dispatchReceiverParameter!!)
                    }
                )
            )
        }

        if (superFunction is IrSimpleFunction && functionByType.name != superFunction.name) {
            buildFun {
                this.name = superFunction.name
                returnType = given.key.type
            }.apply {
                dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
                this.parent = contextImpl
                contextImpl.addChild(this)
                overriddenSymbols += superFunction.symbol
                body = irBuilderTmp().run {
                    irExprBody(
                        irCall(functionByType).apply {
                            dispatchReceiver = irGet(dispatchReceiverParameter!!)
                        }
                    )
                }
            }
        }

        return expression
    }

    private fun childContextExpression(given: GivenChildContext): ContextExpression {
        return { c ->
            irCall(given.factory.constructors.single()).apply {
                putValueArgument(0, c[contextImpl])
            }
        }
    }

    private fun calleeContextExpression(given: GivenCalleeContext): ContextExpression {
        return { c ->
            given.contextImpl?.constructors?.single()
                ?.let {
                    irCall(it).apply {
                        putValueArgument(0, c[contextImpl])
                    }
                } ?: c[contextImpl]
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
                        pluginContext.referenceFunctions(
                            FqName("kotlin.collections.mutableMapOf")
                        ).first { it.owner.valueParameters.isEmpty() })
                )
                val mapType = pluginContext.referenceClass(
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
                        pluginContext.referenceFunctions(
                            FqName("kotlin.collections.mutableSetOf")
                        ).first { it.owner.valueParameters.isEmpty() })
                )
                val collectionType = pluginContext.referenceClass(
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
                        if (index == 0 && given.function.extensionReceiverParameter != null) {
                            extensionReceiver = expression()
                        } else {
                            putValueArgument(
                                index - if (given.function.extensionReceiverParameter != null) 1 else 0,
                                expression()
                            )
                        }
                    }

                    putValueArgument(valueArgumentsCount - 1, c[contextImpl])
                }

                return call
            }

            if (given.explicitParameters.isNotEmpty()) {
                irLambda(given.key.type) { function ->
                    var index = 0
                    val parametersMap = given.explicitParameters
                        .associateWith { parameter ->
                            val paramIndex = index++
                            {
                                irGet(
                                    function.valueParameters[paramIndex]
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

    private fun selfContextExpression(given: GivenSelfContext): ContextExpression {
        return { c -> c[given.context] }
    }

}

class ContextExpressionContext(
    private val expressionsByContext: Map<IrClass, () -> IrExpression>
) {

    operator fun get(context: IrClass) = expressionsByContext[context]!!()

    companion object {
        @Reader
        operator fun invoke(
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
                        currentContext.irBuilderTmp()
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
