package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

class GivenExpressions(
    private val parent: GivenExpressions?,
    private val injektContext: InjektContext,
    private val contextImpl: IrClass
) {

    private val givenExpressions = mutableMapOf<Key, ContextExpression>()

    fun getGivenExpression(given: Given): ContextExpression {
        givenExpressions[given.key]?.let { return it }

        if (given.targetContext != null &&
            given.targetContext != contextImpl.superTypes.first().classOrNull!!.owner
        ) {
            return parent!!.getGivenExpression(given)
                .also { givenExpressions[given.key] = it }
        }

        val rawExpression = when (given) {
            is GivenFunction -> givenExpression(given)
            is GivenInstance -> inputExpression(given)
            is GivenMap -> mapExpression(given)
            is GivenNull -> nullExpression()
            is GivenSet -> setExpression(given)
        }

        val finalExpression = if (given.targetContext == null) rawExpression else ({ c ->
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
                                    rawExpression {
                                        irGet(contextImpl.thisReceiver!!)
                                    }
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
                    c(),
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
            this.body =
                DeclarationIrBuilder(injektContext, symbol).run {
                    irExprBody(finalExpression(this) { irGet(dispatchReceiverParameter!!) })
                }
        }

        val expression: ContextExpression = {
            irCall(function).apply {
                dispatchReceiver = it()
            }
        }

        givenExpressions[given.key] = expression

        return expression
    }

    private fun inputExpression(
        given: GivenInstance
    ): ContextExpression = { irGetField(it(), given.inputField) }

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
                                putValueArgument(valueArgumentsCount - 1, c())
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
                                putValueArgument(valueArgumentsCount - 1, c())
                            }
                        )
                    }
                }

                +irGet(tmpSet)
            }
        }
    }

    private fun nullExpression(): ContextExpression = { irNull() }

    private fun givenExpression(given: GivenFunction): ContextExpression {
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

                    putValueArgument(valueArgumentsCount - 1, c())
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
