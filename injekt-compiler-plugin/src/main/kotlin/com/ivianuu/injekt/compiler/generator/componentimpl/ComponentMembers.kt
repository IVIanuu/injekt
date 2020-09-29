package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.SimpleType
import com.ivianuu.injekt.compiler.generator.Type
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Given
class GivenStatements(private val owner: ComponentImpl) {

    private val parent = owner.factoryImpl.parent?.statements
    private val statementsByType = mutableMapOf<Type, ComponentStatement>()

    fun getFunction(
        type: Type,
        name: Name,
        isOverride: Boolean,
        statement: ComponentStatement,
    ): ComponentFunction {
        val existing = owner.members.firstOrNull {
            it is ComponentFunction && it.name == name
        } as? ComponentFunction
        existing?.let {
            if (isOverride) it.isOverride = true
            return it
        }
        val function = ComponentFunction(
            name = name,
            isOverride = isOverride,
            type = type,
            statement = statement
        )
        owner.members += function
        return function
    }

    fun getGivenStatement(given: GivenNode): ComponentStatement {
        statementsByType[given.type]?.let {
            getFunction(
                type = given.type,
                name = given.type.uniqueTypeName(),
                isOverride = false,
                statement = it
            )
            return it
        }

        val rawStatement = if (given.owner != owner) {
            parent!!.getGivenStatement(given)
        } else {
            when (given) {
                is ChildFactoryGivenNode -> childFactoryExpression(given)
                is CallableGivenNode -> callableExpression(given)
                is InputGivenNode -> inputExpression(given)
                is MapGivenNode -> mapExpression(given)
                is NullGivenNode -> nullExpression()
                is SelfGivenNode -> selfContextExpression(given)
                is SetGivenNode -> setExpression(given)
            }
        }

        val finalStatement = if (given.targetComponent == null ||
            given.owner != owner
        ) rawStatement else ({
            val property = ComponentProperty(
                name = given.type.uniqueTypeName(),
                type = SimpleType(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
                initializer = { emit("this") },
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

        val functionName = given.type.uniqueTypeName()

        getFunction(
            type = given.type,
            name = functionName,
            isOverride = false,
            statement = finalStatement
        )

        val statement: ComponentStatement = { emit("this@${owner.name}.${functionName}()") }

        statementsByType[given.type] = statement

        return statement
    }

    private fun childFactoryExpression(given: ChildFactoryGivenNode): ComponentStatement =
        { emit("${given.childFactoryImpl.name}()") }

    private fun inputExpression(
        given: InputGivenNode,
    ): ComponentStatement = { emit("this@${given.owner.name}.${given.name}") }

    private fun mapExpression(given: MapGivenNode): ComponentStatement {
        return {
            emit("run ")
            braced {
                emitLine("val result = mutableMapOf<Any?, Any?>()")
                given.entries.forEach { (callable, receiver) ->
                    emit("result.putAll(")
                    emitCallableInvocation(callable, receiver, emptyList())
                    emitLine(")")
                }
                emitLine("result as ${given.type.render()}")
            }
        }
    }

    private fun setExpression(given: SetGivenNode): ComponentStatement {
        return {
            emit("run ")
            braced {
                emitLine("val result = mutableSetOf<Any?>()")
                given.elements.forEach { (callable, receiver) ->
                    emit("result.addAll(")
                    emitCallableInvocation(callable, receiver, emptyList())
                    emitLine(")")
                }
                emitLine("result as ${given.type.render()}")
            }
        }
    }

    private fun nullExpression(): ComponentStatement = { emit("null") }

    private fun callableExpression(given: CallableGivenNode): ComponentStatement {
        return {
            if (given.callable.valueParameters.isNotEmpty()) {
                emit("{ ")
                given.callable.valueParameters.forEachIndexed { index, parameter ->
                    emit("p$index: ${parameter.type.render()}")
                    if (index != given.callable.valueParameters.lastIndex) emit(", ")
                }
                emitLine(" ->")
                emitCallableInvocation(
                    given.callable,
                    given.moduleAccessStatement,
                    given.callable.valueParameters.mapIndexed { index, _ ->
                        { emit("p$index") }
                    }
                )
                emitLine()
                emitLine("}")
            } else {
                emitCallableInvocation(
                    given.callable,
                    given.moduleAccessStatement,
                    emptyList()
                )
            }
        }
    }

    private fun selfContextExpression(given: SelfGivenNode): ComponentStatement =
        { emit("this@${given.component.name}") }

}

private fun CodeBuilder.emitCallableInvocation(
    callable: Callable,
    receiver: ComponentStatement?,
    parameters: List<ComponentStatement>,
) {
    when {
        receiver != null -> {
            receiver()
            emit(".${callable.name}")
        }
        callable.objectReceiver != null -> {
            emit("${callable.objectReceiver.render()}.${callable.name}")
        }
        callable.valueParameters.any { it.isExtensionReceiver } -> {
            parameters.first()()
            emit(".${callable.name}")
        }
        else -> emit(callable.fqName)
    }
    if (callable.isCall) {
        emit("(")
        parameters
            .drop(if (callable.valueParameters.firstOrNull()?.isExtensionReceiver == true) 1 else 0)
            .forEachIndexed { index, parameter ->
                parameter()
                if (index != parameters.lastIndex) emit(", ")
            }
        emit(")")
    }
}
