package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding
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

@Binding
class GivenStatements(@Assisted private val owner: ComponentImpl) {

    private val parent = owner.parent?.statements
    private val statementsByType = mutableMapOf<TypeRef, ComponentStatement>()

    fun getCallable(
        type: TypeRef,
        name: Name,
        isOverride: Boolean,
        body: ComponentStatement,
        isProperty: Boolean,
        isSuspend: Boolean
    ): ComponentCallable {
        val existing = owner.members.firstOrNull {
            it is ComponentCallable && it.name == name
        } as? ComponentCallable
        existing?.let {
            if (isOverride) it.isOverride = true
            return it
        }
        val callable = ComponentCallable(
            name = name,
            isOverride = isOverride,
            type = type,
            body = body,
            isProperty = isProperty,
            isSuspend = isSuspend,
            initializer = null,
            isMutable = false
        )
        owner.members += callable
        return callable
    }

    fun getGivenStatement(given: GivenNode): ComponentStatement {
        val isSuspend = given is CallableGivenNode && given.callable.isSuspend
        statementsByType[given.type]?.let {
            getCallable(
                type = given.type,
                name = given.type.uniqueTypeName(),
                isOverride = false,
                body = it,
                isSuspend = isSuspend,
                isProperty = !isSuspend
            )
            return it
        }

        val rawStatement = if (given.owner != owner) {
            parent!!.getGivenStatement(given)
        } else {
            when (given) {
                is ChildImplGivenNode -> childFactoryExpression(given)
                is CallableGivenNode -> callableExpression(given)
                is FunctionAliasGivenNode -> functionAliasExpression(given)
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
                val property = ComponentCallable(
                    name = "_${given.type.uniqueTypeName()}".asNameId(),
                    type = SimpleTypeRef(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
                    initializer = { emit("this") },
                    isMutable = true,
                    body = null,
                    isOverride = false,
                    isProperty = true,
                    isSuspend = false,
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

        val callableName = given.type.uniqueTypeName()

        getCallable(
            type = given.type,
            name = callableName,
            isOverride = false,
            body = finalStatement,
            isProperty = !isSuspend,
            isSuspend = isSuspend
        )

        val statement: ComponentStatement = {
            emit("this@${owner.name}.$callableName")
            if (isSuspend) emit("()")
        }

        statementsByType[given.type] = statement

        return statement
    }

    private fun childFactoryExpression(given: ChildImplGivenNode): ComponentStatement = {
        emit("::${given.childComponentImpl.name}")
    }

    private fun mapExpression(given: MapGivenNode): ComponentStatement = {
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

    private fun setExpression(given: SetGivenNode): ComponentStatement = {
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

    private fun nullExpression(): ComponentStatement = { emit("null") }

    private fun callableExpression(given: CallableGivenNode): ComponentStatement = {
        if (given.valueParameters.any { it.isAssisted }) {
            emit("{ ")
            given.valueParameters
                .filter { it.isAssisted }
                .forEachIndexed { index, parameter ->
                    emit("p$index: ${parameter.type.render()}")
                    if (index != given.valueParameters.lastIndex) emit(", ")
                }
            emitLine(" ->")
            var assistedIndex = 0
            var nonAssistedIndex = 0
            emitCallableInvocation(
                given.callable,
                given.receiver,
                given.valueParameters.map { parameter ->
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
                given.receiver,
                given.dependencies.map { getGivenStatement(owner.graph.getGiven(it)) }
            )
        }
    }

    private fun functionAliasExpression(given: FunctionAliasGivenNode): ComponentStatement = {
        emit("{ ")
        given.valueParameters
            .filter { it.isAssisted }
            .forEachIndexed { index, parameter ->
                emit("p$index: ${parameter.type.render()}")
                if (index != given.valueParameters.lastIndex) emit(", ")
            }
        emitLine(" ->")
        var assistedIndex = 0
        var nonAssistedIndex = 0
        emitCallableInvocation(
            given.callable,
            given.receiver,
            given.valueParameters.map { parameter ->
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
    }

    private fun providerExpression(given: ProviderGivenNode): ComponentStatement = {
        braced { getGivenStatement(owner.graph.getGiven(given.dependencies.single()))() }
    }

    private fun selfContextExpression(given: SelfGivenNode): ComponentStatement = {
        emit("this@${given.component.name}")
    }
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
