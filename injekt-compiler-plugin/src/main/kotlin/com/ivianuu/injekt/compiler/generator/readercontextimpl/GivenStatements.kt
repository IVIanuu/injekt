package com.ivianuu.injekt.compiler.generator.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.FqNameTypeRef
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName

@Given
class GivenStatements(private val owner: ContextImpl) {

    private val parent = owner.statements
    private val statementsByType =
        mutableMapOf<com.ivianuu.injekt.compiler.generator.TypeRef, ContextStatement>()

    fun getGivenStatement(
        given: GivenNode,
        isOverride: Boolean
    ): ContextStatement {
        statementsByType[given.type]?.let { return it }

        val rawStatement = if (given.owner != owner) {
            parent!!.getGivenStatement(given, false)
        } else {
            when (given) {
                // todo is GivenCalleeContext -> calleeContextExpression(given)
                //is ChildContextGivenNode -> childContextExpression(given)
                //is GivenFunction -> functionExpression(given)
                is InstanceGivenNode -> inputExpression(given)
                // is GivenMap -> mapExpression(given)
                //is GivenNull -> nullExpression()
                //is GivenSelfContext -> selfContextExpression(given)
                //is GivenSet -> setExpression(given)
                else -> TODO()
            }
        }

        val finalStatement = if (given.targetContext == null ||
            given.owner != owner
        ) rawStatement else ({
            val property = ContextProperty(
                name = given.type.uniqueTypeName(),
                type = FqNameTypeRef(FqName("kotlin.Any"), isMarkedNullable = true),
                initializer = { emit("this") },
                owner = owner,
                isMutable = true
            ).also { owner.members += it }

            emitLine("var value = this@${owner.name}.${property.name}")
            emitLine("if (value !== this@${owner.name}) return value as ${given.type}")
            emit("synchronized(this) ")
            braced {
                emitLine("value = this@${owner.name}.${property.name}")
                emitLine("if (value !== this@${owner.name}) return value as ${given.type}")
                emit("value = ")
                rawStatement()
                emitLine()
                emitLine("this@${owner.name}.${property.name} = value")
                emitLine("return value as ${given.type}")
            }
        })

        val functionByType = ContextFunction(
            name = given.type.uniqueTypeName(),
            isOverride = isOverride,
            type = given.type,
            owner = owner,
            statement = finalStatement
        )

        owner.members += functionByType


        /*if (superFunction is IrSimpleFunction && functionByType.name != superFunction.name) {
            buildFun {
                this.name = superFunction.name
                returnType = given.type
            }.apply {
                dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
                this.parent = contextImpl
                contextImpl.addChild(this)
                overriddenSymbols += superFunction.symbol
                body = irBuilder().run {
                    irExprBody(
                        irCall(functionByType).apply {
                            dispatchReceiver = irGet(dispatchReceiverParameter!!)
                        }
                    )
                }
            }
        }*/

        val statement: ContextStatement = {
            emit("this@${owner.name}.${functionByType.name}()")
        }

        statementsByType[given.type] = statement

        return statement
    }

    /*
    private fun childContextExpression(given: GivenChildContext): ContextStatement {
        return { c ->
            irCall(given.factory.constructors.single()).apply {
                putValueArgument(0, c[contextImpl])
            }
        }
    }

    private fun calleeContextExpression(given: GivenCalleeContext): ContextStatement {
        return { c ->
            given.contextImpl?.constructors?.single()
                ?.let {
                    irCall(it).apply {
                        putValueArgument(0, c[contextImpl])
                    }
                } ?: c[contextImpl]
        }
    }*/

    private fun inputExpression(
        given: InstanceGivenNode
    ): ContextStatement {
        return {
            emit("this@${given.owner.name}.${given.name}")
        }
    }

    /**
    private fun mapExpression(given: GivenMap): ContextStatement {
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

    private fun setExpression(given: GivenSet): ContextStatement {
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

    private fun nullExpression(): ContextStatement = { irNull() }

    private fun functionExpression(given: GivenFunction): ContextStatement {
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
    irLambda(given.type) { function ->
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

    private fun selfContextExpression(given: GivenSelfContext): ContextStatement {
    return { c -> c[given.context] }
    }*/

}
