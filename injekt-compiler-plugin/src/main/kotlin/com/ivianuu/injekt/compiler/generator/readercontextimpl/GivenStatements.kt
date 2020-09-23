package com.ivianuu.injekt.compiler.generator.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName

@Given
class GivenStatements(private val owner: ContextImpl) {

    private val parent = owner.factoryImpl.parent?.statements
    private val statementsByType = mutableMapOf<TypeRef, ContextStatement>()

    fun getGivenStatement(
        given: GivenNode,
        isOverride: Boolean
    ): ContextStatement {
        statementsByType[given.type]?.let { return it }

        val rawStatement = if (given.owner != owner) {
            parent!!.getGivenStatement(given, false)
        } else {
            when (given) {
                is CalleeContextGivenNode -> calleeContextExpression(given)
                is ChildContextGivenNode -> childContextExpression(given)
                is CallableGivenNode -> functionExpression(given)
                is InputGivenNode -> inputExpression(given)
                is MapGivenNode -> TODO() //childContextExpression(given)
                is NullGivenNode -> nullExpression()
                is SelfGivenNode -> selfContextExpression(given)
                is SetGivenNode -> TODO() //childContextExpression(given)
            }
        }

        val finalStatement = if (given.targetContext == null ||
            given.owner != owner
        ) rawStatement else ({
            val property = ContextProperty(
                name = given.type.uniqueTypeName(),
                type = SimpleTypeRef(FqName("kotlin.Any"), isMarkedNullable = true),
                initializer = { emit("this") },
                owner = owner,
                isMutable = true
            ).also { owner.members += it }

            emit("run ")
            braced {
                emitLine("var value = this@${owner.name}.${property.name}")
                emitLine("if (value !== this@${owner.name}) return@run value as ${given.type.render()}")
                emit("synchronized(this) ")
                braced {
                    emitLine("value = this@${owner.name}.${property.name}")
                    emitLine("if (value !== this@${owner.name}) return@run value as ${given.type.render()}")
                    emit("value = ")
                    rawStatement()
                    emitLine()
                    emitLine("this@${owner.name}.${property.name} = value")
                    emitLine("return@run value as ${given.type.render()}")
                }
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

    private fun childContextExpression(given: ChildContextGivenNode): ContextStatement =
        { emit("${given.childFactoryImpl.name}()") }

    private fun calleeContextExpression(given: CalleeContextGivenNode): ContextStatement =
        given.calleeContextStatement

    private fun inputExpression(
        given: InputGivenNode
    ): ContextStatement = { emit("this@${given.owner.name}.${given.name}") }

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
    }*/

    private fun nullExpression(): ContextStatement = { emit("null") }

    private fun functionExpression(given: CallableGivenNode): ContextStatement {
        fun createExpression(parameters: List<ContextStatement>): ContextStatement {
            return {
                if (given.callable.receiver != null) {
                    emit("${given.callable.receiver.render()}.${given.callable.name}")
                } else {
                    emit(given.callable.fqName)
                }
                if (!given.callable.isPropertyAccessor) {
                    emit("(")
                    parameters.forEachIndexed { index, parameter ->
                        parameter()
                        if (index != parameters.lastIndex) emit(", ")
                    }
                    emit(")")
                }
            }
        }

        return if (given.callable.parameters.isNotEmpty()) {
            val statement: ContextStatement = {
                emit("{ ")
                given.callable.parameters.forEachIndexed { index, parameter ->
                    emit("p$index: ${parameter.typeRef.render()}")
                    if (index != given.callable.parameters.lastIndex) emit(", ")
                }
                emitLine(" ->")
                createExpression(given.callable.parameters.mapIndexed { index, _ ->
                    { emit("p$index") }
                }).invoke(this)
                emitLine()
                emitLine("}")
            }
            statement
        } else {
            createExpression(emptyList())
        }
    }

    private fun selfContextExpression(given: SelfGivenNode): ContextStatement =
        { emit("this@${given.context.name}") }

}
