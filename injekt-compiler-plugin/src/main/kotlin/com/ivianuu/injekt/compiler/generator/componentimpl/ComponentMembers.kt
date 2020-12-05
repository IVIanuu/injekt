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

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.ValueParameterRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.fullyExpandedType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.toClassifierRef
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Binding
class ComponentStatements(
    private val declarationStore: DeclarationStore,
    private val moduleDescriptor: ModuleDescriptor,
    private val owner: ComponentImpl
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
        eager: Boolean,
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
        val callable = ComponentCallable(
            name = name,
            isOverride = isOverride,
            type = type,
            body = if (!eager) body else null,
            isProperty = isProperty,
            callableKind = callableKind,
            initializer = if (eager) body else null,
            isMutable = false,
            isInline = isInline,
            canBePrivate = canBePrivate,
            valueParameters = emptyList(),
            typeParameters = emptyList()
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
                eager = binding.eager,
                isInline = false,
                canBePrivate = false
            )
            return it
        }

        // ask the parent for the expression if were not the owners members
        if (binding.owner != owner) {
            return {
                componentExpression(parent!!.owner)()
                emit(".")
                parent!!.getBindingExpression(request)()
            }
        }

        // ensure all dependencies will get initialized before us
        binding
            .interceptors
            .flatMap { it.dependencies }
            .filterNot {
                it.lazy || it.type == binding.type || it.type == owner.componentType
            }
            .filter { owner.graph.getBinding(it) !is MissingBindingNode }
            .forEach { getBindingExpression(it) }
        binding
            .dependencies
            .filterNot {
                it.lazy || it.type == binding.type ||
                        it.type == owner.componentType
            }
            .filter { owner.graph.getBinding(it) !is MissingBindingNode }
            .forEach { getBindingExpression(it) }

        val rawExpression = when (binding) {
            is AssistedBindingNode -> assistedExpression(binding)
            is ChildComponentBindingNode -> childFactoryExpression(binding)
            is CallableBindingNode -> callableExpression(binding)
            is FunBindingNode -> funBindingExpression(binding)
            is InputBindingNode -> inputExpression(binding)
            is MapBindingNode -> mapExpression(binding)
            is MissingBindingNode -> if (binding.type.isMarkedNullable) {
                { emit("null") }
            } else {
                error("Cannot create expression for a non null missing binding ${binding.type}")
            }
            is ProviderBindingNode -> providerExpression(binding)
            is SelfBindingNode -> selfExpression(binding)
            is SetBindingNode -> setExpression(binding)
        }

        val maybeScopedExpression = if (binding.targetComponent == null || binding.eager)
            rawExpression else
            scoped(binding.type, binding.callableKind, false,
                binding.targetComponent!!, binding.interceptors.isNotEmpty(), rawExpression)

        val maybeInterceptedExpression = if (binding.interceptors.isEmpty()) maybeScopedExpression
        else intercepted(binding.type, binding.callableKind, binding.interceptors, !binding.eager,
            binding.interceptors.isNotEmpty(), maybeScopedExpression)

        val requestForType = owner.requests
            .filterNot { it in owner.assistedRequests }
            .firstOrNull { it.type == binding.type }

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
            body = maybeInterceptedExpression,
            isProperty = isProperty,
            callableKind = requestForType?.callableKind ?: callableKind,
            eager = binding.eager,
            isInline = requestForType in owner.assistedRequests || (!isOverride && binding.inline),
            canBePrivate = requestForType in owner.assistedRequests || !isOverride
        )

        val accessExpression: ComponentExpression = {
            emit("$callableName")
            if (callableKind == Callable.CallableKind.SUSPEND) emit("()")
        }

        expressionsByType[binding.type] = accessExpression

        return accessExpression
    }

    private fun assistedExpression(binding: AssistedBindingNode): ComponentExpression = {
        emit("{ ")
        binding.assistedTypes
            .forEachIndexed { index, assistedType ->
                emit("p$index: ${assistedType.render(expanded = true)}")
                if (index != binding.assistedTypes.lastIndex) emit(", ")
            }
        emitLine(" ->")
        indented {
            fun emitNewInstance() {
                emit("${binding.childComponent.name}(this, ")
                binding.assistedTypes.forEachIndexed { index, assistedType ->
                    emit("p$index")
                    if (index != binding.assistedTypes.lastIndex) emit(", ")
                }
                emit(")")
                emitLine(".invoke()")
            }
            if (binding.scoped) {
                scoped(binding.type.typeArguments.last(), binding.callableKind, false,
                    binding.targetComponent!!, false) {
                    emitNewInstance()
                }
            } else {
                emitNewInstance()
            }
            emitLine()
        }
        emitLine("}")
    }

    private fun childFactoryExpression(binding: ChildComponentBindingNode): ComponentExpression = {
        emit("{ ")
        val params = binding.type.typeArguments.dropLast(1)
        params.forEachIndexed { index, param ->
            emit("p$index: ${param.render(expanded = true)}")
            if (index != params.lastIndex) emit(", ")
        }
        emitLine(" ->")
        indented {
            emit("${binding.childComponent.name}(this")
            if (params.isNotEmpty()) {
                emit(", ")
                params.indices.forEach { paramIndex ->
                    emit("p$paramIndex")
                    if (paramIndex != params.lastIndex) emit(", ")
                }
            }
            emitLine(")")
        }

        emit("}")
    }

    private fun funBindingExpression(binding: FunBindingNode): ComponentExpression = {
        emit("{ ")

        val funApiParameters = binding.callable.valueParameters
            .filter { it.isFunApi }

        funApiParameters
            .filterNot { it.parameterKind == ValueParameterRef.ParameterKind.EXTENSION_RECEIVER }
            .forEachIndexed { index, param ->
                emit("${param.name}: ${param.type.render(expanded = true)}")
                if (index != funApiParameters.lastIndex) emit(", ")
            }
        emitLine(" ->")

        indented {
            emitCallableInvocation(
                callable = binding.callable,
                type = binding.type.fullyExpandedType.typeArguments.last(),
                arguments = binding.callable.valueParameters.mapNotNull { valueParameter ->
                    if (valueParameter in funApiParameters) {
                        val expression: ComponentExpression = {
                            if (valueParameter.parameterKind == ValueParameterRef.ParameterKind.EXTENSION_RECEIVER) emit("this")
                            else emit(valueParameter.name)
                        }
                        valueParameter to (valueParameter.type to expression)
                    } else {
                        val request = with(owner.graph) {
                            valueParameter.toBindingRequest(binding.callable, emptyMap())
                        }
                        val dependencyBinding = owner.graph.getBinding(request)
                        if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                        valueParameter to (dependencyBinding.type to getBindingExpression(request))
                    }
                }.toMap()
            )
        }

        emit("}")
    }

    private fun inputExpression(binding: InputBindingNode): ComponentExpression = {
        emit("i_${binding.type.uniqueTypeName()}")
    }

    private fun mapExpression(binding: MapBindingNode): ComponentExpression = {
        fun Callable.emit() {
            emitCallableInvocation(
                this,
                binding.type,
                valueParameters
                    .mapNotNull { parameter ->
                        val requests = binding.dependenciesByEntry[this]!!
                        val request = requests.first { it.origin.shortName() == parameter.name }
                        val dependencyBinding = owner.graph.getBinding(request)
                        if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                        parameter to (dependencyBinding.type to getBindingExpression(request))
                    }
                    .toMap()
            )
        }
        if (binding.entries.size == 1) {
            binding.entries.single().emit()
        } else {
            emit("mutableMapOf<${binding.type.fullyExpandedType.typeArguments[0].render(expanded = true)}, " +
                    "${binding.type.fullyExpandedType.typeArguments[1].render(expanded = true)}>().also ")
            braced {
                binding.entries.forEach { entry ->
                    emit("it.putAll(")
                    entry.emit()
                    emitLine(")")
                }
            }
        }
    }

    private fun setExpression(binding: SetBindingNode): ComponentExpression = {
        fun Callable.emit() {
            emitCallableInvocation(
                this,
                binding.type,
                valueParameters
                    .mapNotNull { parameter ->
                        val requests = binding.dependenciesByElement[this]!!
                        val request = requests.first { it.origin.shortName() == parameter.name }
                        val dependencyBinding = owner.graph.getBinding(request)
                        if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                        parameter to (dependencyBinding.type to getBindingExpression(request))
                    }
                    .toMap()
            )
        }
        if (binding.elements.size == 1) {
            binding.elements.single().emit()
        } else {
            emit("mutableSetOf<${binding.type.fullyExpandedType.typeArguments[0].render(expanded = true)}>().also ")
            braced {
                binding.elements.forEach { element ->
                    emit("it.addAll(")
                    element.emit()
                    emitLine(")")
                }
            }
        }
    }

    private fun callableExpression(binding: CallableBindingNode): ComponentExpression = {
        emitCallableInvocation(
            binding.callable,
            binding.type,
            binding.callable.valueParameters
                .mapNotNull { parameter ->
                    val request = binding.dependencies.single {
                        it.origin.shortName() == parameter.name
                    }
                    val dependencyBinding = owner.graph.getBinding(request)
                    if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                    parameter to (dependencyBinding.type to getBindingExpression(request))
                }
                .toMap()
        )
    }

    private fun providerExpression(binding: ProviderBindingNode): ComponentExpression = {
        braced { getBindingExpression(binding.dependencies.single())() }
    }

    private fun selfExpression(binding: SelfBindingNode): ComponentExpression = {
        emit("this")
    }

    private fun intercepted(
        type: TypeRef,
        callableKind: Callable.CallableKind,
        interceptors: List<InterceptorNode>,
        cacheProvider: Boolean,
        clearableProvider: Boolean,
        create: ComponentExpression
    ): ComponentExpression {
        val initializer: CodeBuilder.() -> Unit = {
            emitLine()
            fun InterceptorNode.emit(prevExpression: ComponentExpression) {
                var dependencyIndex = 0
                emitCallableInvocation(
                    callable,
                    callable.type,
                    callable.valueParameters
                        .mapNotNull { valueParameter ->
                            val pair: Pair<ValueParameterRef, Pair<TypeRef?, ComponentExpression>> = valueParameter to (when {
                                valueParameter.type == callable.type -> {
                                    (valueParameter.type to prevExpression)
                                }
                                else -> {
                                    val dependencyRequest = dependencies[dependencyIndex++]
                                    val dependencyBinding = owner.graph.getBinding(dependencyRequest)
                                    if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                                    (dependencyBinding.type to getBindingExpression(dependencyRequest))
                                }
                            })
                            pair
                        }
                        .toMap()
                )
            }
            if (interceptors.size == 1) {
                interceptors.single().emit {
                    braced {
                        create()
                    }
                }
            } else {
                interceptors.fold<InterceptorNode, ComponentExpression>({
                    braced {
                        create()
                    }
                }) { acc, next -> { next.emit(acc) } }()
            }
        }
        return if (cacheProvider) {
            val providerType = when (callableKind) {
                Callable.CallableKind.DEFAULT -> moduleDescriptor.builtIns.getFunction(0)
                    .toClassifierRef().defaultType.typeWith(listOf(type))
                Callable.CallableKind.SUSPEND -> moduleDescriptor.builtIns.getSuspendFunction(0)
                    .toClassifierRef().defaultType.typeWith(listOf(type))
                Callable.CallableKind.COMPOSABLE -> moduleDescriptor.builtIns.getFunction(0)
                    .toClassifierRef().defaultType.typeWith(listOf(type))
                    .copy(isComposable = true)
            }.let {
                if (clearableProvider) it.copy(isMarkedNullable = true) else it
            }
            val providerProperty = ComponentCallable(
                name = "${type.uniqueTypeName()}_Provider".asNameId(),
                type = providerType,
                initializer = initializer,
                isMutable = clearableProvider,
                body = null,
                isOverride = false,
                isProperty = true,
                callableKind = Callable.CallableKind.DEFAULT,
                isInline = false,
                canBePrivate = true,
                valueParameters = emptyList(),
                typeParameters = emptyList()
            ).also { owner.members += it }

            val expression: ComponentExpression = {
                emit("${providerProperty.name}")
                if (clearableProvider) emit("!!")
                emit("()")
            }
            expression
        } else {
            val expression: ComponentExpression = {
                initializer()
                emit(".invoke()")
            }
            expression
        }
    }

    private fun scoped(
        type: TypeRef,
        callableKind: Callable.CallableKind,
        frameworkType: Boolean,
        scopeComponentType: TypeRef,
        clearProvider: Boolean,
        create: ComponentExpression
    ): ComponentExpression {
        var scopeComponent = owner
        while(scopeComponent.componentType != scopeComponentType) {
            scopeComponent = scopeComponent.parent!!
        }
        val scopeComponentExpression = componentExpression(scopeComponent)

        val name = "${if (frameworkType) "_" else ""}_${type.uniqueTypeName()}".asNameId()
        val cacheProperty = scopeComponent.members.firstOrNull {
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
            canBePrivate = true,
            valueParameters = emptyList(),
            typeParameters = emptyList()
        ).also { scopeComponent.members += it }

        if (callableKind == Callable.CallableKind.SUSPEND) {
            val mutexType = declarationStore.classDescriptorForFqName(InjektFqNames.Mutex)
                .toClassifierRef().defaultType
            scopeComponent.members.firstOrNull {
                it is ComponentCallable &&
                        !it.isProperty &&
                        it.name.asString() == "_mutex"
            } as? ComponentCallable ?: ComponentCallable(
                name = "_mutex".asNameId(),
                type = mutexType,
                initializer = null,
                isMutable = true,
                body = scoped(
                    mutexType,
                    Callable.CallableKind.DEFAULT,
                    true,
                    scopeComponentType,
                    false
                ) {
                    emit("${InjektFqNames.Mutex}()")
                },
                isOverride = false,
                isProperty = false,
                callableKind = Callable.CallableKind.DEFAULT,
                isInline = false,
                canBePrivate = true,
                valueParameters = emptyList(),
                typeParameters = emptyList()
            ).also { scopeComponent.members += it }
        }

        return {
            emit("run scope@ ")
            braced {
                emit("var value = ")
                scopeComponentExpression()
                emitLine(".${cacheProperty.name}")
                emit("if (value !== ")
                scopeComponentExpression()
                emitLine(") return@scope value as ${type.render(expanded = true)}")
                fun emitInvocation() {
                    emit("value = ")
                    scopeComponentExpression()
                    emitLine(".${cacheProperty.name}")
                    emit("if (value !== ")
                    scopeComponentExpression()
                    emitLine(") return@scope value as ${type.render(expanded = true)}")
                    emit("value = ")
                    create()
                    emitLine()
                    if (clearProvider) {
                        emit("${type.uniqueTypeName()}_Provider".asNameId())
                        emitLine(" = null")
                    }
                    scopeComponentExpression()
                    emitLine(".${cacheProperty.name} = value")
                    emitLine("return@scope value as ${type.render(expanded = true)}")
                }
                when (callableKind) {
                    Callable.CallableKind.SUSPEND -> {
                        emit("_mutex().withLock ")
                        braced { emitInvocation() }
                    }
                    Callable.CallableKind.DEFAULT -> {
                        emit("synchronized(")
                        scopeComponentExpression()
                        emit(") ")
                        braced { emitInvocation() }
                    }
                    // todo what to do here?
                    Callable.CallableKind.COMPOSABLE -> emitInvocation()
                }
            }
        }
    }

    private fun componentExpression(component: ComponentImpl): ComponentExpression {
        return {
            if (component == owner) {
                emit("this")
            } else {
                var current = owner
                while (current != component) {
                    emit("_parent")
                    current = current.parent!!
                    if (current != component) emit(".")
                }
            }
        }
    }

    private fun CodeBuilder.emitCallableInvocation(
        callable: Callable,
        type: TypeRef,
        arguments: Map<ValueParameterRef, Pair<TypeRef?, ComponentExpression?>>
    ) {
        val finalCallable = if (callable.visibility == DescriptorVisibilities.PROTECTED &&
            callable.valueParameters.any {
                it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER
                        && it.type != owner.componentType
            }
        ) {
            val ownerType = callable.valueParameters.first {
                it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER
            }.type
            var current: ComponentImpl? = owner
            while (current != null) {
                if (current.componentType == ownerType) break
                current = current.parent
            }
            val accessorName = "_${callable.name}".asNameId()
            current!!.members.firstOrNull {
                it is ComponentCallable &&
                        it.name == accessorName
            } ?: ComponentCallable(
                name = accessorName,
                type = callable.type,
                isProperty = !callable.isCall,
                callableKind = callable.callableKind,
                initializer = null,
                body = {
                    emit("${callable.name}")
                    if (callable.isCall) {
                        emit("(")
                        callable.valueParameters
                            .filterNot { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                            .forEachIndexed { index, parameter ->
                                emit(parameter.name)
                                if (index != callable.valueParameters.lastIndex) emit(", ")
                            }
                        emit(")")
                    }
                },
                isMutable = false,
                isOverride = false,
                isInline = false,
                canBePrivate = true,
                valueParameters = callable.valueParameters
                    .filterNot { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                    .map {
                        ComponentCallable.ValueParameter(
                            it.name,
                            it.type
                        )
                    },
                typeParameters = callable.typeParameters
                    .map {
                        ComponentCallable.TypeParameter(
                            it.fqName.shortName(),
                            it.superTypes
                        )
                    }
            ).also { current.members += it }
            callable.copy(
                name = accessorName,
                visibility = DescriptorVisibilities.INTERNAL
            )
        } else {
            callable
        }
        fun emitArguments() {
            if (finalCallable.isCall) {
                if (finalCallable.typeParameters.isNotEmpty()) {
                    val substitutionMap = getSubstitutionMap(
                        listOf(type to finalCallable.originalType) +
                                finalCallable.valueParameters
                                    .mapNotNull {
                                        val argumentType = arguments[it]?.first
                                        if (argumentType != null) {
                                            argumentType to it.originalType
                                        } else null
                                    },
                        finalCallable.typeParameters
                    )

                    check(finalCallable.typeParameters.all { it in substitutionMap }) {
                        "Couldn't resolve all type arguments ${substitutionMap.map {
                            it.key.fqName to it.value
                        }} missing ${finalCallable.typeParameters.filter {
                            it !in substitutionMap
                        }.map { it.fqName }} in $finalCallable"
                    }

                    emit("<")
                    finalCallable.typeParameters.forEachIndexed { index, typeParameter ->
                        val typeArgument = substitutionMap[typeParameter]!!
                        emit(typeArgument.render(expanded = true))
                        if (index != finalCallable.typeParameters.lastIndex) emit(", ")
                    }
                    emit(">")
                }
                emitLine("(")
                indented {
                    var argumentsIndex = 0
                    val nonNullArgumentsCount = finalCallable.valueParameters
                        .filter { it.parameterKind == ValueParameterRef.ParameterKind.VALUE_PARAMETER }
                        .count { it in arguments }
                    val isFunctionInvoke = callable.valueParameters
                        .firstOrNull { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                        ?.type
                        ?.let { it.isFunction || it.isSuspendFunction } ?: false
                    finalCallable.valueParameters
                        .filter { it.parameterKind == ValueParameterRef.ParameterKind.VALUE_PARAMETER }
                        .forEach { parameter ->
                            val argument = arguments[parameter]
                            if (argument != null) {
                                if (!isFunctionInvoke) emit("${parameter.name} = ")
                                argument.second!!(this)
                                if (argumentsIndex++ != nonNullArgumentsCount) emitLine(",")
                            }
                            else if (!parameter.hasDefault) {
                                if (!isFunctionInvoke) emit("${parameter.name} = null")
                                if (argumentsIndex++ != nonNullArgumentsCount) emitLine(",")
                            }
                        }
                }
                emitLine(")")
            }
        }
        val dispatchReceiverParameter = finalCallable.valueParameters.firstOrNull {
            it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER
        }
        val extensionReceiverParameter = finalCallable.valueParameters.firstOrNull {
            it.parameterKind == ValueParameterRef.ParameterKind.EXTENSION_RECEIVER
        }
        if (dispatchReceiverParameter != null) {
            emit("with(")
            emitOrNull(arguments[dispatchReceiverParameter]?.second)
            emit(") ")
            braced {
                if (extensionReceiverParameter != null) {
                    emit("with(")
                    emitOrNull(arguments[extensionReceiverParameter]?.second)
                    emit(") ")
                    braced {
                        emit(finalCallable.name)
                        emitArguments()
                    }
                } else {
                    emit(finalCallable.name)
                    emitArguments()
                }
            }
        } else {
            if (extensionReceiverParameter != null) {
                emit("with(")
                emitOrNull(arguments[extensionReceiverParameter]?.second)
                emit(") ")
                braced {
                    emit(finalCallable.name)
                    emitArguments()
                }
            } else {
                emit(finalCallable.fqName)
                emitArguments()
            }
        }
    }

    private fun CodeBuilder.emitOrNull(expression: ComponentExpression?) {
        expression?.invoke(this) ?: emit("null")
    }

}
