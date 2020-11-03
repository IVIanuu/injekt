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
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.TypeTranslator
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.renderExpanded
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Binding
class ComponentStatements(
    private val declarationStore: DeclarationStore,
    private val owner: @Assisted ComponentImpl,
    private val typeTranslator: TypeTranslator
) {

    private val parent = owner.parent?.statements
    private val expressionsByType = mutableMapOf<TypeRef, ComponentExpression>()

    fun getCallable(
        type: TypeRef,
        name: Name,
        isOverride: Boolean,
        body: ComponentExpression,
        isProperty: Boolean,
        callableKind: Callable.CallableKind,
        cacheable: Boolean,
        isInline: Boolean,
        canBePrivate: Boolean
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
            isMutable = false,
            isInline = isInline,
            canBePrivate = canBePrivate
        )
        owner.members += callable
        return callable
    }

    fun getBindingExpression(request: BindingRequest): ComponentExpression {
        val binding = owner.graph.getBinding(request)
        val callableKind = binding.callableKind
        expressionsByType[binding.type]?.let {
            // todo not sure why we do this :D
            getCallable(
                type = binding.type,
                name = binding.type.uniqueTypeName(),
                isOverride = false,
                body = it,
                callableKind = callableKind,
                isProperty = callableKind != Callable.CallableKind.SUSPEND,
                cacheable = binding.cacheable,
                isInline = false,
                canBePrivate = false
            )
            return it
        }

        val rawExpression = if (binding.owner != owner) {
            parent!!.getBindingExpression(request)
        } else {
            when (binding) {
                is AssistedBindingNode -> assistedExpression(binding)
                is ChildComponentBindingNode -> childFactoryExpression(binding)
                is CallableBindingNode -> callableExpression(binding)
                is DelegateBindingNode -> delegateExpression(binding)
                is InputBindingNode -> inputExpression(binding)
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
                scoped(binding.type, binding.callableKind, false) {
                    rawExpression()
                }
            })

        val requestForType = owner.requests
            .firstOrNull { it.type == binding.type }

        if (binding.inlineMode == BindingNode.InlineMode.EXPRESSION &&
            requestForType == null &&
            rawExpression == finalExpression) {
            return finalExpression
        }

        val callableName = requestForType
            ?.name ?: binding.type.uniqueTypeName()

        val isProperty = if (requestForType != null) !requestForType.isCall
        else binding.callableKind != Callable.CallableKind.SUSPEND

        val isOverride = requestForType != null &&
                requestForType !in owner.assistedRequests

        getCallable(
            type = binding.type,
            name = callableName,
            isOverride = isOverride,
            body = finalExpression,
            isProperty = isProperty,
            callableKind = requestForType?.callableKind ?: callableKind,
            cacheable = binding.cacheable,
            isInline = !isOverride &&
                    binding.inlineMode == BindingNode.InlineMode.FUNCTION,
            canBePrivate = !isOverride && requestForType !in owner.assistedRequests
        )

        val expression: ComponentExpression = {
            emit("this@${owner.name}.$callableName")
            if (callableKind == Callable.CallableKind.SUSPEND) emit("()")
        }

        expressionsByType[binding.type] = expression

        return expression
    }

    private fun assistedExpression(binding: AssistedBindingNode): ComponentExpression = {
        emit("{ ")
        binding.assistedTypes
            .forEachIndexed { index, assistedType ->
                emit("p$index: ${assistedType.renderExpanded()}")
                if (index != binding.assistedTypes.lastIndex) emit(", ")
            }
        emitLine(" ->")
        fun emitNewInstance() {
            emit("${binding.childComponent.name}(")
            binding.assistedTypes.forEachIndexed { index, assistedType ->
                emit("p$index")
                if (index != binding.assistedTypes.lastIndex) emit(", ")
            }
            emit(")")
            emitLine(".invoke()")
        }
        if (binding.targetComponent != null) {
            scoped(binding.type.typeArguments.last(), binding.callableKind, false) {
                emitNewInstance()
            }
        } else {
            emitNewInstance()
        }
        emitLine()
        emitLine("}")
    }

    private fun childFactoryExpression(binding: ChildComponentBindingNode): ComponentExpression = {
        emit("::${binding.childComponent.name}")
    }

    private fun delegateExpression(binding: DelegateBindingNode): ComponentExpression = {
        val delegate = getBindingExpression(binding.dependencies.single())
        emit("(")
        delegate()
        emit(" as ${binding.type})")
    }

    private fun inputExpression(binding: InputBindingNode): ComponentExpression = {
        emit("this@${binding.owner.name}.input${binding.index}")
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
                                BindingRequest(
                                    it.type,
                                    callable.fqName.child(it.name)
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
                                BindingRequest(
                                    it.type,
                                    callable.fqName.child(it.name)
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
        emitCallableInvocation(
            binding.callable,
            binding.receiver,
            binding.callable.valueParameters.zip(binding.dependencies).map { (parameter, request) ->
                val raw = getBindingExpression(request)
                if (parameter.type.isInlineProvider) {
                    {
                        emit("{ ")
                        raw()
                        emit(" }")
                    }
                } else raw
            }
        )
    }

    private fun providerExpression(binding: ProviderBindingNode): ComponentExpression = {
        braced { getBindingExpression(binding.dependencies.single())() }
    }

    private fun selfContextExpression(binding: SelfBindingNode): ComponentExpression = {
        emit("this@${binding.component.name}")
    }

    private fun CodeBuilder.scoped(
        type: TypeRef,
        callableKind: Callable.CallableKind,
        frameworkType: Boolean,
        create: CodeBuilder.() -> Unit
    ) {
        val name = "${if (frameworkType) "_" else ""}_${type.uniqueTypeName()}".asNameId()
        val cacheProperty = owner.nonAssistedComponent.members.firstOrNull {
            it is ComponentCallable && it.name == name
        } as? ComponentCallable ?: ComponentCallable(
            name = name,
            type = SimpleTypeRef(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
            initializer = { emit("this") },
            isMutable = true,
            body = null,
            isOverride = false,
            isProperty = true,
            callableKind = Callable.CallableKind.DEFAULT,
            isInline = false,
            canBePrivate = true
        ).also { owner.nonAssistedComponent.members += it }

        if (callableKind == Callable.CallableKind.SUSPEND) {
            val mutexType = typeTranslator.toClassifierRef(
                declarationStore.classDescriptorForFqName(InjektFqNames.Mutex)
            ).defaultType
            owner.members.firstOrNull {
                it is ComponentCallable &&
                        !it.isProperty &&
                        it.name.asString() == "_mutex"
            } as? ComponentCallable ?: ComponentCallable(
                name = "_mutex".asNameId(),
                type = mutexType,
                initializer = null,
                isMutable = true,
                body = {
                    scoped(
                        mutexType,
                        Callable.CallableKind.DEFAULT,
                        true
                    ) {
                        emit("${InjektFqNames.Mutex}()")
                    }
                },
                isOverride = false,
                isProperty = false,
                callableKind = Callable.CallableKind.DEFAULT,
                isInline = false,
                canBePrivate = true
            ).also { owner.members += it }
        }

        emit("run ")
        braced {
            emitLine("var value = this@${owner.nonAssistedComponent.name}.${cacheProperty.name}")
            emitLine("if (value !== this@${owner.nonAssistedComponent.name}) return@run value as ${type.renderExpanded()}")
            fun emitInvocation() {
                emitLine("value = this@${owner.nonAssistedComponent.name}.${cacheProperty.name}")
                emitLine("if (value !== this@${owner.nonAssistedComponent.name}) return@run value as ${type.renderExpanded()}")
                emit("value = ")
                create()
                emitLine()
                emitLine("this@${owner.nonAssistedComponent.name}.${cacheProperty.name} = value")
                emitLine("return@run value as ${type.renderExpanded()}")
            }
            when (callableKind) {
                Callable.CallableKind.SUSPEND -> {
                    emit("_mutex().withLock ")
                    braced { emitInvocation() }
                }
                Callable.CallableKind.DEFAULT -> {
                    emit("synchronized(this) ")
                    braced { emitInvocation() }
                }
                // todo what to do here?
                Callable.CallableKind.COMPOSABLE -> emitInvocation()
            }
        }
    }
}

fun CodeBuilder.emitCallableInvocation(
    callable: Callable,
    receiver: ComponentExpression?,
    arguments: List<ComponentExpression>
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
