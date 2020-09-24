package com.ivianuu.injekt.compiler.generator.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.ClassifierRef
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
                is MapGivenNode -> mapExpression(given)
                is NullGivenNode -> nullExpression()
                is SelfGivenNode -> selfContextExpression(given)
                is SetGivenNode -> setExpression(given)
            }
        }

        val finalStatement = if (given.targetContext == null ||
            given.owner != owner
        ) rawStatement else ({
            val property = ContextProperty(
                name = given.type.uniqueTypeName(),
                type = SimpleTypeRef(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
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

        val functionByTypeName = given.type.uniqueTypeName()
        val functionByType = ContextFunction(
            name = functionByTypeName,
            isOverride = isOverride,
            type = given.type,
            statement = finalStatement
        )

        owner.members += functionByType

        val statement: ContextStatement = {
            emit("this@${owner.name}.${functionByType.name}()")
        }

        statementsByType[given.type] = statement

        return statement
    }

    private fun childContextExpression(given: ChildContextGivenNode): ContextStatement =
        { emit("${given.childFactoryImpl.name}()") }

    private fun calleeContextExpression(given: CalleeContextGivenNode): ContextStatement =
        { given.calleeContextStatement(this) }

    private fun inputExpression(
        given: InputGivenNode
    ): ContextStatement = { emit("this@${given.owner.name}.${given.name}") }

    private fun mapExpression(given: MapGivenNode): ContextStatement {
        return {
            emit("run ")
            braced {
                emitLine("val result = mutableMapOf<Any?, Any?>()")
                given.entries.forEach {
                    emitLine("result.putAll(${it.fqName}())")
                }
                emitLine("result as ${given.type.render()}")
            }
        }
    }

    private fun setExpression(given: SetGivenNode): ContextStatement {
        return {
            emit("run ")
            braced {
                emitLine("val result = mutableSetOf<Any?>()")
                given.elements.forEach {
                    emitLine("result.addAll(${it.fqName}())")
                }
                emitLine("result as ${given.type.render()}")
            }
        }
    }

    private fun nullExpression(): ContextStatement = { emit("null") }

    private fun functionExpression(given: CallableGivenNode): ContextStatement {
        fun createExpression(parameters: List<ContextStatement>): ContextStatement {
            return {
                when {
                    given.givenSetAccessStatement != null -> {
                        given.givenSetAccessStatement!!()
                        emit(".${given.callable.name}")
                    }
                    given.callable.receiver != null -> {
                        emit("${given.callable.receiver.render()}.${given.callable.name}")
                    }
                    given.callable.valueParameters.any { it.isExtensionReceiver } -> {
                        parameters.first()()
                        emit(".${given.callable.name}")
                    }
                    else -> emit(given.callable.fqName)
                }
                if (!given.callable.isPropertyAccessor) {
                    emit("(")
                    parameters
                        .drop(if (given.callable.valueParameters.firstOrNull()?.isExtensionReceiver == true) 1 else 0)
                        .forEachIndexed { index, parameter ->
                            parameter()
                            if (index != parameters.lastIndex) emit(", ")
                        }
                    emit(")")
                }
            }
        }

        return if (given.callable.valueParameters.isNotEmpty()) {
            val statement: ContextStatement = {
                emit("{ ")
                given.callable.valueParameters.forEachIndexed { index, parameter ->
                    emit("p$index: ${parameter.typeRef.render()}")
                    if (index != given.callable.valueParameters.lastIndex) emit(", ")
                }
                emitLine(" ->")
                createExpression(given.callable.valueParameters.mapIndexed { index, _ ->
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
