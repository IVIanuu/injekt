package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName

class GivenExpressions(
    private val injektContext: InjektContext,
    private val contextImpl: IrClass,
    private val graph: GivensGraph
) {

    private val givenExpressions = mutableMapOf<Key, ContextExpression>()

    fun getGivenExpression(request: GivenRequest): ContextExpression {
        givenExpressions[request.key]?.let { return it }

        val rawExpression = when (val node = graph.getGivenNode(request)) {
            is FunctionGivenNode -> givenExpression(node)
            is InstanceGivenNode -> inputExpression(node)
            is MapGivenNode -> mapExpression(node)
            is NullGivenNode -> nullExpression()
            is SetGivenNode -> setExpression(node)
        }

        val function = buildFun {
            this.name = request.key.type.uniqueTypeName()
            returnType = request.key.type
        }.apply {
            dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
            this.parent = contextImpl
            contextImpl.addChild(this)
            this.body =
                DeclarationIrBuilder(injektContext, symbol).run {
                    irExprBody(rawExpression(this) { irGet(dispatchReceiverParameter!!) })
                }
        }

        val expression: ContextExpression = {
            irCall(function).apply {
                dispatchReceiver = it()
            }
        }

        givenExpressions[request.key] = expression

        return expression
    }

    private fun inputExpression(
        node: InstanceGivenNode
    ): ContextExpression = { irGetField(it(), node.inputField) }

    private fun mapExpression(node: MapGivenNode): ContextExpression {
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
                node.functions.forEach { function ->
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

    private fun setExpression(node: SetGivenNode): ContextExpression {
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
                node.functions.forEach { function ->
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

    private fun givenExpression(node: FunctionGivenNode): ContextExpression {
        return { c ->
            fun createExpression(parametersMap: Map<IrValueParameter, () -> IrExpression?>): IrExpression {
                val call = if (node.function is IrConstructor) {
                    IrConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        node.function.returnType,
                        node.function.symbol,
                        node.function.constructedClass.typeParameters.size,
                        node.function.typeParameters.size,
                        node.function.valueParameters.size
                    )
                } else {
                    IrCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        node.function.returnType,
                        node.function.symbol,
                        node.function.typeParameters.size,
                        node.function.valueParameters.size
                    )
                }
                call.apply {
                    if (node.function.dispatchReceiverParameter != null) {
                        dispatchReceiver = if (node.givenSetAccessExpression != null) {
                            node.givenSetAccessExpression!!(c)
                        } else {
                            irGetObject(
                                node.function.dispatchReceiverParameter!!.type.classOrNull!!
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

                return call /*if (binding.scopeComponent != null) {
                    irCall(
                        injektContext.injektSymbols.storage
                            .owner
                            .functions
                            .first { it.name.asString() == "scope" }
                    ).apply {
                        dispatchReceiver = createBindingExpression(
                            context,
                            graph,
                            BindingRequest(
                                key = binding.scopeComponent.defaultType.asKey(),
                                requestingKey = binding.key,
                                requestOrigin = binding.origin
                            )
                        )(c)
                        putValueArgument(
                            0,
                            irInt(binding.key.hashCode())
                        )
                        putValueArgument(
                            1,
                            irLambda(
                                injektContext.tmpFunction(0)
                                    .typeWith(binding.key.type)
                            ) { call }
                        )
                    }
                } else {*/

                //}
            }

            if (node.explicitParameters.isNotEmpty()) {
                irLambda(node.key.type) { function ->
                    val parametersMap = node.explicitParameters
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
