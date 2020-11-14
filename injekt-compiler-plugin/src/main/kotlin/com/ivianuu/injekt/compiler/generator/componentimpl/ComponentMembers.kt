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
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.fullyExpandedType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Binding
class ComponentStatements(
    private val declarationStore: DeclarationStore,
    private val moduleDescriptor: ModuleDescriptor,
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
                cacheable = binding.cacheable,
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
            .decorators
            .flatMap { it.dependencies }
            .filter { owner.graph.getBinding(it) !is MissingBindingNode }
            .forEach { getBindingExpression(it) }
        binding
            .dependencies
            .filterNot { it.lazy }
            .filter { owner.graph.getBinding(it) !is MissingBindingNode }
            .forEach { getBindingExpression(it) }

        val rawExpression = when (binding) {
            is AssistedBindingNode -> assistedExpression(binding)
            is ChildComponentBindingNode -> childFactoryExpression(binding)
            is CallableBindingNode -> callableExpression(binding)
            is FunBindingNode -> funBindingExpression(binding)
            is InputBindingNode -> inputExpression(binding)
            is MapBindingNode -> mapExpression(binding)
            is MissingBindingNode -> error("Cannot create expression for a missing binding ${binding.type}")
            is ProviderBindingNode -> providerExpression(binding)
            is SelfBindingNode -> selfExpression(binding)
            is SetBindingNode -> setExpression(binding)
        }

        val maybeScopedExpression = if (binding.targetComponent == null || binding.cacheable)
            rawExpression else
            scoped(binding.type, binding.callableKind, false, binding.targetComponent!!, rawExpression)

        val maybeDecoratedExpression = if (binding.decorators.isEmpty()) maybeScopedExpression
        else decorated(binding.type, binding.callableKind, binding.decorators, maybeScopedExpression)

        val requestForType = owner.requests
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
            body = maybeDecoratedExpression,
            isProperty = isProperty,
            callableKind = requestForType?.callableKind ?: callableKind,
            cacheable = binding.cacheable,
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
            if (binding.targetComponent != null) {
                scoped(binding.type.typeArguments.last(), binding.callableKind, false, binding.targetComponent!!) {
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
            .filterNot { it.isExtensionReceiver }
            .forEachIndexed { index, param ->
                emit("${param.name}: ${param.type.render(expanded = true)}")
                if (index != funApiParameters.lastIndex) emit(", ")
            }
        emitLine(" ->")

        indented {
            emitCallableInvocation(
                callable = binding.callable,
                type = binding.type.fullyExpandedType.typeArguments.last(),
                receiver = null,
                owner = null,
                arguments = binding.callable.valueParameters.mapNotNull { valueParameter ->
                    if (valueParameter in funApiParameters) {
                        val expression: ComponentExpression = {
                            if (valueParameter.isExtensionReceiver) emit("this")
                            else emit(valueParameter.name)
                        }
                        valueParameter.name to (valueParameter.type to expression)
                    } else {
                        val request = valueParameter.toBindingRequest(binding.callable, emptyMap())
                        val dependencyBinding = owner.graph.getBinding(request)
                        if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                        valueParameter.name to (dependencyBinding.type to getBindingExpression(request))
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
        fun CallableWithReceiver.emit() {
            emitCallableInvocation(
                callable,
                binding.type,
                receiver,
                declaredInComponent,
                callable.valueParameters
                    .mapNotNull { parameter ->
                        parameter.name to if (parameter.argName != null) {
                            parameter.type to callable.valueArgs[parameter.argName]!!
                        } else {
                            val requests = binding.dependenciesByEntry[callable]!!
                            val request = requests.first { it.origin.shortName() == parameter.name }
                            val dependencyBinding = owner.graph.getBinding(request)
                            if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                            dependencyBinding.type to getBindingExpression(request)
                        }
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
        fun CallableWithReceiver.emit() {
            emitCallableInvocation(
                callable,
                binding.type,
                receiver,
                declaredInComponent,
                callable.valueParameters
                    .mapNotNull { parameter ->
                        parameter.name to if (parameter.argName != null) {
                            parameter.type to callable.valueArgs[parameter.argName]!!
                        } else {
                            val requests = binding.dependenciesByElement[callable]!!
                            val request = requests.first { it.origin.shortName() == parameter.name }
                            val dependencyBinding = owner.graph.getBinding(request)
                            if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                            dependencyBinding.type to getBindingExpression(request)
                        }
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
            binding.receiver,
            binding.declaredInComponent,
            binding.callable.valueParameters
                .mapNotNull { parameter ->
                    parameter.name to if (parameter.argName != null) {
                        parameter.type to binding.callable.valueArgs[parameter.argName]!!
                    } else {
                        val request = binding.dependencies.single {
                            it.origin.shortName() == parameter.name
                        }
                        val dependencyBinding = owner.graph.getBinding(request)
                        if (dependencyBinding is MissingBindingNode) return@mapNotNull null
                        dependencyBinding.type to getBindingExpression(request)
                    }
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

    private fun decorated(
        type: TypeRef,
        callableKind: Callable.CallableKind,
        decorators: List<DecoratorNode>,
        create: ComponentExpression
    ): ComponentExpression {
        val providerType = when (callableKind) {
            Callable.CallableKind.DEFAULT -> typeTranslator.toClassifierRef(
                moduleDescriptor.builtIns.getFunction(0)
            ).defaultType.typeWith(listOf(type))
            Callable.CallableKind.SUSPEND -> typeTranslator.toClassifierRef(
                moduleDescriptor.builtIns.getSuspendFunction(0)
            ).defaultType.typeWith(listOf(type))
            Callable.CallableKind.COMPOSABLE -> typeTranslator.toClassifierRef(
                moduleDescriptor.builtIns.getFunction(0)
            ).defaultType.typeWith(listOf(type)).copy(isComposable = true)
        }

        val initializer: CodeBuilder.() -> Unit = {
            val decoratorFactoryType = typeTranslator.toClassifierRef(
                moduleDescriptor.builtIns.getFunction(1)
            ).defaultType.typeWith(listOf(providerType, providerType))
            emitLine()
            fun DecoratorNode.emit(prevExpression: ComponentExpression) {
                var dependencyIndex = 0
                emitCallableInvocation(
                    callable,
                    callable.type,
                    receiver,
                    declaredInComponent,
                    callable.valueParameters
                        .mapNotNull { valueParameter ->
                            val pair: Pair<Name, Pair<TypeRef?, ComponentExpression>> = valueParameter.name to (when {
                                valueParameter.name in callable.valueArgs -> {
                                    (valueParameter.type to callable.valueArgs[valueParameter.name]!!)
                                }
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
            if (decorators.size == 1) {
                decorators.single().emit {
                    braced {
                        create()
                    }
                }
            } else {
                emitLine("listOf<${decoratorFactoryType.render(expanded = true)}>(")
                indented {
                    decorators.forEachIndexed { index, decorator ->
                        emitLine("{ _prev ->")
                        indented {
                            decorator.emit { emit("_prev") }
                        }
                        emit(" }")
                        if (index != decorators.lastIndex) emitLine(", ")
                    }
                }
                emitLine(")")
                emit(".fold(")
                braced {
                    create()
                }
                emitLine(") { acc, next -> ")
                emitLine("next(acc)")
                emitLine("}")
            }
        }
        val name = "${type.uniqueTypeName()}_Provider".asNameId()
        val providerProperty = ComponentCallable(
            name = name,
            type = providerType,
            initializer = initializer,
            isMutable = false,
            body = null,
            isOverride = false,
            isProperty = true,
            callableKind = Callable.CallableKind.DEFAULT,
            isInline = false,
            canBePrivate = true,
            valueParameters = emptyList(),
            typeParameters = emptyList()
        ).also { owner.members += it }

        return {
            emit("${providerProperty.name}()")
        }
    }

    private fun scoped(
        type: TypeRef,
        callableKind: Callable.CallableKind,
        frameworkType: Boolean,
        scopeComponentType: TypeRef,
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
            val mutexType = typeTranslator.toClassifierRef(
                declarationStore.classDescriptorForFqName(InjektFqNames.Mutex)
            ).defaultType
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
                    scopeComponentType
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
        receiver: ComponentExpression?,
        owner: ComponentImpl?,
        arguments: Map<Name, Pair<TypeRef?, ComponentExpression>?>
    ) {
        fun emitArguments() {
            if (callable.isCall) {
                if (callable.typeParameters.isNotEmpty()) {
                    val substitutionMap = getSubstitutionMap(
                        callable.typeParameters
                            .mapNotNull { (callable.typeArgs[it] ?: return@mapNotNull null) to it.defaultType } +
                                listOf(type to callable.originalType) +
                                callable.valueParameters
                                    .mapNotNull {
                                        val argumentType = arguments[it.name]?.first
                                        if (argumentType != null) {
                                            argumentType to it.originalType
                                        } else null
                                    },
                        callable.typeParameters
                    )

                    check(callable.typeParameters.all { it.defaultType in substitutionMap }) {
                        "Couldn't resolve all type arguments ${substitutionMap.map {
                            it.key.classifier.fqName to it.value
                        }} missing ${callable.typeParameters.filter {
                            it.defaultType !in substitutionMap
                        }.map { it.fqName }} in $callable"
                    }

                    emit("<")
                    callable.typeParameters.forEachIndexed { index, typeParameter ->
                        val typeArgument = substitutionMap[typeParameter.defaultType]!!
                        emit(typeArgument.render(expanded = true))
                        if (index != callable.typeParameters.lastIndex) emit(", ")
                    }
                    emit(">")
                }
                emitLine("(")
                indented {
                    var argumentsIndex = 0
                    val nonNullArgumentsCount = callable.valueParameters
                        .filterNot { it.isExtensionReceiver }
                        .count { it.name in arguments }
                    callable.valueParameters
                        .filterNot { it.isExtensionReceiver }
                        .forEach { parameter ->
                            val argument = arguments[parameter.name]
                            if (argument != null) {
                                emit("${parameter.name} = ")
                                argument.second(this)
                                if (argumentsIndex++ != nonNullArgumentsCount) emitLine(",")
                            }
                            else if (!parameter.hasDefault) {
                                emit("${parameter.name} = null")
                                if (argumentsIndex++ != nonNullArgumentsCount) emitLine(",")
                            }
                        }
                }
                emitLine(")")
            }
        }
        if (owner != null) {
            emit("with(")
            val isObjectCallable = callable.receiver?.isObject ?: false
            if (!isObjectCallable) componentExpression(owner)()
            if (receiver != null) {
                if (!isObjectCallable) emit(".")
                receiver()
            }
            emit(") ")
            braced {
                if (callable.valueParameters.any { it.isExtensionReceiver }) {
                    emit("with(")
                    emitOrNull(arguments[callable.valueParameters.first().name]?.second)
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
                emitOrNull(arguments[callable.valueParameters.first().name]?.second)
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

    private fun CodeBuilder.emitOrNull(expression: ComponentExpression?) {
        expression?.invoke(this) ?: emit("null")
    }

}
