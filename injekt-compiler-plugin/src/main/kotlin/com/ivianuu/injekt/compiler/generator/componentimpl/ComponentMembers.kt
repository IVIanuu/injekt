/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.renderExpanded
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Binding
class ComponentStatements(private val owner: @Assisted ComponentImpl) {

    private val parent = owner.parent?.statements
    private val expressionsByType = mutableMapOf<TypeRef, ComponentExpression>()

    fun getCallable(
        type: TypeRef,
        name: Name,
        isOverride: Boolean,
        body: ComponentExpression,
        isProperty: Boolean,
        callableKind: Callable.CallableKind,
        cacheable: Boolean
    ): ComponentCallable {
        val existing = owner.members.firstOrNull {
            it is ComponentCallable && it.name == name
        } as? ComponentCallable
        existing?.let {
            if (isOverride) it.isOverride = true
            return it
        }
        val cache = cacheable && isProperty && callableKind == Callable.CallableKind.DEFAULT
        val callable = ComponentCallable(
            name = name,
            isOverride = isOverride,
            type = type,
            body = if (!cache) body else null,
            isProperty = isProperty,
            callableKind = callableKind,
            initializer = if (cache) body else null,
            isMutable = false
        )
        owner.members += callable
        return callable
    }

    fun getBindingExpression(binding: BindingNode): ComponentExpression {
        val callableKind = when (binding) {
            is CallableBindingNode -> {
                if (binding.valueParameters.none { it.isAssisted })
                    binding.callable.callableKind
                else Callable.CallableKind.DEFAULT
            }
            else -> Callable.CallableKind.DEFAULT
        }
        expressionsByType[binding.type]?.let {
            getCallable(
                type = binding.type,
                name = binding.type.uniqueTypeName(),
                isOverride = false,
                body = it,
                callableKind = callableKind,
                isProperty = callableKind != Callable.CallableKind.SUSPEND,
                cacheable = binding.cacheable
            )
            return it
        }

        val rawExpression = if (binding.owner != owner) {
            parent!!.getBindingExpression(binding)
        } else {
            when (binding) {
                is ChildImplBindingNode -> childFactoryExpression(binding)
                is CallableBindingNode -> callableExpression(binding)
                is FunBindingNode -> funBindingExpression(binding)
                is MapBindingNode -> mapExpression(binding)
                is NullBindingNode -> nullExpression()
                is ProviderBindingNode -> providerExpression(binding)
                is SelfBindingNode -> selfContextExpression(binding)
                is SetBindingNode -> setExpression(binding)
            }
        }

        val finalExpression = if (binding.targetComponent == null ||
            binding.owner != owner || binding.cacheable
        ) rawExpression else (
            {
                val property = ComponentCallable(
                    name = "_${binding.type.uniqueTypeName()}".asNameId(),
                    type = SimpleTypeRef(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
                    initializer = { emit("this") },
                    isMutable = true,
                    body = null,
                    isOverride = false,
                    isProperty = true,
                    callableKind = Callable.CallableKind.DEFAULT,
                ).also { owner.members += it }

                emit("run ")
                braced {
                    emitLine("var value = this@${owner.name}.${property.name}")
                    emitLine("if (value !== this@${owner.name}) return@run value as ${binding.type.renderExpanded()}")
                    emit("synchronized(this) ")
                    braced {
                        emitLine("value = this@${owner.name}.${property.name}")
                        emitLine("if (value !== this@${owner.name}) return@run value as ${binding.type.renderExpanded()}")
                        emit("value = ")
                        rawExpression()
                        emitLine()
                        emitLine("this@${owner.name}.${property.name} = value")
                        emitLine("return@run value as ${binding.type.renderExpanded()}")
                    }
                }
            }
            )

        val requestForType = owner.requests
            .firstOrNull { it.type == binding.type }

        val callableName = requestForType
            ?.name ?: binding.type.uniqueTypeName()

        val isProperty = requestForType?.isCall?.not() ?: if (binding is CallableBindingNode)
            binding.callable.callableKind != Callable.CallableKind.SUSPEND else true

        getCallable(
            type = binding.type,
            name = callableName,
            isOverride = requestForType != null,
            body = finalExpression,
            isProperty = isProperty,
            callableKind = requestForType?.callableKind ?: callableKind,
            cacheable = binding.cacheable
        )

        val expression: ComponentExpression = {
            emit("this@${owner.name}.$callableName")
            if (callableKind == Callable.CallableKind.SUSPEND) emit("()")
        }

        expressionsByType[binding.type] = expression

        return expression
    }

    private fun childFactoryExpression(binding: ChildImplBindingNode): ComponentExpression = {
        emit("::${binding.childComponentImpl.name}")
    }

    private fun mapExpression(binding: MapBindingNode): ComponentExpression = {
        emit("run ")
        braced {
            emitLine("val result = mutableMapOf<Any?, Any?>()")
            binding.entries.forEach { (callable, receiver) ->
                emit("result.putAll(")
                emitCallableInvocation(
                    callable,
                    receiver,
                    callable.valueParameters
                        .map {
                            getBindingExpression(
                                owner.graph.getBinding(
                                    BindingRequest(
                                        it.type,
                                        callable.fqName.child(it.name)
                                    )
                                )
                            )
                        }
                )
                emitLine(")")
            }
            emitLine("result as ${binding.type.renderExpanded()}")
        }
    }

    private fun setExpression(binding: SetBindingNode): ComponentExpression = {
        emit("run ")
        braced {
            emitLine("val result = mutableSetOf<Any?>()")
            binding.elements.forEach { (callable, receiver) ->
                emit("result.addAll(")
                emitCallableInvocation(
                    callable,
                    receiver,
                    callable.valueParameters
                        .map {
                            getBindingExpression(
                                owner.graph.getBinding(
                                    BindingRequest(
                                        it.type,
                                        callable.fqName.child(it.name)
                                    )
                                )
                            )
                        }
                )
                emitLine(")")
            }
            emitLine("result as ${binding.type.renderExpanded()}")
        }
    }

    private fun nullExpression(): ComponentExpression = { emit("null") }

    private fun callableExpression(binding: CallableBindingNode): ComponentExpression = {
        if (binding.valueParameters.any { it.isAssisted }) {
            emit("{ ")
            binding.valueParameters
                .filter { it.isAssisted }
                .forEachIndexed { index, parameter ->
                    emit("p$index: ${parameter.type.renderExpanded()}")
                    if (index != binding.valueParameters.lastIndex) emit(", ")
                }
            emitLine(" ->")
            var assistedIndex = 0
            var nonAssistedIndex = 0
            emitCallableInvocation(
                binding.callable,
                binding.receiver,
                binding.valueParameters.map { parameter ->
                    if (parameter.isAssisted) {
                        { emit("p${assistedIndex++}") }
                    } else {
                        getBindingExpression(
                            owner.graph.getBinding(
                                BindingRequest(
                                    binding.dependencies[nonAssistedIndex++].type,
                                    binding.callable.fqName.child(parameter.name)
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
                binding.callable,
                binding.receiver,
                binding.dependencies.map { getBindingExpression(owner.graph.getBinding(it)) }
            )
        }
    }

    private fun funBindingExpression(binding: FunBindingNode): ComponentExpression = {
        emit("{ ")
        val assistedParameters = binding.valueParameters
            .filter { it.isAssisted }
        val assistedValueParameters = assistedParameters
            .filterNot { it.isExtensionReceiver }
        assistedValueParameters
            .forEachIndexed { index, parameter ->
                emit("p$index: ${parameter.type.renderExpanded()}")
                if (index != assistedValueParameters.lastIndex) emit(", ")
            }
        emitLine(" ->")
        var assistedIndex = 0
        var nonAssistedIndex = 0
        emitCallableInvocation(
            binding.callable,
            binding.receiver,
            binding.valueParameters.map { parameter ->
                when {
                    parameter.isAssisted -> {
                        {
                            if (parameter.isExtensionReceiver) {
                                emit("this")
                            } else {
                                emit("p${assistedIndex++}")
                            }
                        }
                    }
                    else -> {
                        getBindingExpression(
                            owner.graph.getBinding(
                                BindingRequest(
                                    binding.dependencies[nonAssistedIndex++].type,
                                    binding.callable.fqName.child(parameter.name)
                                )
                            )
                        )
                    }
                }
            }
        )
        emitLine()
        emitLine("}")
    }

    private fun providerExpression(binding: ProviderBindingNode): ComponentExpression = {
        braced { getBindingExpression(owner.graph.getBinding(binding.dependencies.single()))() }
    }

    private fun selfContextExpression(binding: SelfBindingNode): ComponentExpression = {
        emit("this@${binding.component.name}")
    }
}

private fun CodeBuilder.emitCallableInvocation(
    callable: Callable,
    receiver: ComponentExpression?,
    arguments: List<ComponentExpression>,
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
        if (callable.valueParameters.any { it.isExtensionReceiver }) {
            emit("with(")
            arguments.first()()
            emit(") ")
            braced {
                emit(callable.name)
                emitArguments()
            }
        } else {
            emit(callable.fqName)
            emitArguments()
        }
    }
}
