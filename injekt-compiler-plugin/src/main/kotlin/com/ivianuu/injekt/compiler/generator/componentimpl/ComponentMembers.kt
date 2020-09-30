package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Given
class GivenStatements(
    @Assisted private val owner: ComponentImpl
) {

    private val parent = owner.factoryImpl.parent?.statements
    private val statementsByType = mutableMapOf<TypeRef, ComponentStatement>()

    fun getProperty(
        type: TypeRef,
        name: Name,
        isOverride: Boolean,
        getter: ComponentStatement,
    ): ComponentProperty {
        val existing = owner.members.firstOrNull {
            it is ComponentProperty && it.name == name
        } as? ComponentProperty
        existing?.let {
            if (isOverride) it.isOverride = true
            return it
        }
        val property = ComponentProperty(
            name = name,
            isOverride = isOverride,
            type = type,
            getter = getter,
            initializer = null,
            isMutable = false
        )
        owner.members += property
        return property
    }

    fun getGivenStatement(given: GivenNode): ComponentStatement {
        statementsByType[given.type]?.let {
            getProperty(
                type = given.type,
                name = given.type.uniqueTypeName(),
                isOverride = false,
                getter = it
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
                is ProviderGivenNode -> providerExpression(given)
                is SelfGivenNode -> selfContextExpression(given)
                is SetGivenNode -> setExpression(given)
            }
        }

        val finalStatement = if (given.targetComponent == null ||
            given.owner != owner
        ) rawStatement else (
            {
                val property = ComponentProperty(
                    name = "_${given.type.uniqueTypeName()}".asNameId(),
                    type = SimpleTypeRef(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
                    initializer = { emit("this") },
                    isMutable = true,
                    getter = null,
                    isOverride = false
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
            }
            )

        val propertyName = given.type.uniqueTypeName()

        getProperty(
            type = given.type,
            name = propertyName,
            isOverride = false,
            getter = finalStatement
        )

        val statement: ComponentStatement = { emit("this@${owner.name}.$propertyName") }

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
                    emitCallableInvocation(
                        callable,
                        receiver,
                        callable.valueParameters
                            .map {
                                getGivenStatement(
                                    owner.graph.getGiven(
                                        GivenRequest(
                                            it.type,
                                            callable.fqName.child(it.name)
                                        )
                                    )
                                )
                            }
                    )
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
                    emitCallableInvocation(
                        callable,
                        receiver,
                        callable.valueParameters
                            .map {
                                getGivenStatement(
                                    owner.graph.getGiven(
                                        GivenRequest(
                                            it.type,
                                            callable.fqName.child(it.name)
                                        )
                                    )
                                )
                            }
                    )
                    emitLine(")")
                }
                emitLine("result as ${given.type.render()}")
            }
        }
    }

    private fun nullExpression(): ComponentStatement = { emit("null") }

    private fun callableExpression(given: CallableGivenNode): ComponentStatement {
        return {
            if (given.type.isFunctionAlias ||
                given.callable.valueParameters.any { it.isAssisted }
            ) {
                emit("{ ")
                given.callable.valueParameters
                    .filter { it.isAssisted }
                    .forEachIndexed { index, parameter ->
                        emit("p$index: ${parameter.type.render()}")
                        if (index != given.callable.valueParameters.lastIndex) emit(", ")
                    }
                emitLine(" ->")
                var assistedIndex = 0
                var nonAssistedIndex = 0
                emitCallableInvocation(
                    given.callable,
                    given.moduleAccessStatement,
                    given.callable.valueParameters.map { parameter ->
                        if (parameter.isAssisted) {
                            { emit("p${assistedIndex++}") }
                        } else {
                            getGivenStatement(
                                owner.graph.getGiven(
                                    GivenRequest(
                                        given.dependencies[nonAssistedIndex++].type,
                                        given.callable.fqName.child(parameter.name)
                                    )
                                )
                            )
                        }
                    }
                )
                emitLine()
                emitLine("}")
            } else {
                emitCallableInvocation(
                    given.callable,
                    given.moduleAccessStatement,
                    given.dependencies.map { getGivenStatement(owner.graph.getGiven(it)) }
                )
            }
        }
    }

    private fun providerExpression(given: ProviderGivenNode): ComponentStatement =
        {
            braced { getGivenStatement(owner.graph.getGiven(given.dependencies.single()))() }
        }

    private fun selfContextExpression(given: SelfGivenNode): ComponentStatement =
        { emit("this@${given.component.name}") }
}

private fun CodeBuilder.emitCallableInvocation(
    callable: Callable,
    receiver: ComponentStatement?,
    arguments: List<ComponentStatement>,
) {
    fun emitArguments() {
        if (callable.isCall) {
            emit("(")
            arguments
                .drop(if (callable.valueParameters.firstOrNull()?.isExtensionReceiver == true) 1 else 0)
                .forEachIndexed { index, parameter ->
                    parameter()
                    if (index != arguments.lastIndex) emit(", ")
                }
            emit(")")
        }
    }
    if (receiver != null) {
        emit("with(")
        receiver()
        emit(") ")
        braced {
            if (callable.valueParameters.any { it.isExtensionReceiver }) {
                emit("with(")
                arguments.first()()
                emit(") ")
                braced {
                    emit(callable.name)
                    emitArguments()
                }
            } else {
                emit(callable.name)
                emitArguments()
            }
        }
    } else {
        emit(callable.fqName)
        emitArguments()
    }
}
