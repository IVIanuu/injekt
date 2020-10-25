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
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.ModuleDescriptor
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.isAssignable
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Binding
class BindingGraph(
    private val owner: @Assisted ComponentImpl,
    collectionsFactory: (ComponentImpl, BindingCollections?) -> BindingCollections,
    private val declarationStore: DeclarationStore,
    private val componentImplFactory: (
        TypeRef,
        Name,
        ComponentImpl?,
    ) -> ComponentImpl,
) {

    private val parent = owner.parent?.graph

    private val componentType = owner.componentType

    private val moduleBindingCallables = mutableListOf<CallableWithReceiver>()

    private val collections: BindingCollections = collectionsFactory(owner, parent?.collections)

    val bindingsByRequest = mutableMapOf<TypeRef, BindingNode>()
    val resolvedBindings = mutableListOf<BindingNode>()

    private val chain = mutableListOf<BindingNode>()

    init {
        fun ModuleDescriptor.collectContributions(
            parentCallable: Callable?,
            parentAccessExpression: ComponentExpression,
        ) {
            val thisAccessExpression: ComponentExpression = {
                if (type.classifier.isObject) {
                    emit(type.classifier.fqName)
                } else {
                    parentAccessExpression()
                    if (parentCallable != null) {
                        emit(".")
                        emit("${parentCallable!!.name}")
                        if (parentCallable.isCall) emit("()")
                    }
                }
            }

            for (callable in callables) {
                if (callable.contributionKind == null) continue
                when (callable.contributionKind) {
                    Callable.ContributionKind.BINDING -> moduleBindingCallables += CallableWithReceiver(
                        callable,
                        thisAccessExpression,
                        emptyMap()
                    )
                    Callable.ContributionKind.MAP_ENTRIES -> {
                        collections.addMapEntries(
                            CallableWithReceiver(
                                callable,
                                thisAccessExpression,
                                emptyMap()
                            )
                        )
                    }
                    Callable.ContributionKind.SET_ELEMENTS -> {
                        collections.addSetElements(
                            CallableWithReceiver(
                                callable,
                                thisAccessExpression,
                                emptyMap()
                            )
                        )
                    }
                    Callable.ContributionKind.MODULE -> {
                        declarationStore.moduleForType(callable.type)
                            .collectContributions(callable, thisAccessExpression)
                    }
                }.let {}
            }
        }

        declarationStore.moduleForType(componentType)
            .collectContributions(null) { emit("this@${owner.name}") }
        owner.mergeDeclarations
            .filter { it.isModule }
            .map { declarationStore.moduleForType(it) }
            .onEach { includedModule ->
                if (includedModule.type.classifier.isObject) {
                    includedModule.collectContributions(null) {
                        emit("${includedModule.type.classifier.fqName}")
                    }
                } else {
                    val callable = ComponentCallable(
                        name = includedModule.type.uniqueTypeName(),
                        isOverride = false,
                        type = includedModule.type,
                        body = null,
                        isProperty = true,
                        callableKind = Callable.CallableKind.DEFAULT,
                        initializer = {
                            emit("${includedModule.type.classifier.fqName}")
                            if (!includedModule.type.classifier.isObject)
                                emit("()")
                        },
                        isMutable = false
                    ).also { owner.members += it }

                    includedModule.collectContributions(null) {
                        emit("this@${owner.name}.${callable.name}")
                    }
                }
            }
    }

    fun checkRequests(requests: List<BindingRequest>) {
        requests.forEach { check(it) }
    }

    private fun check(binding: BindingNode) {
        if (binding in chain) {
            val relevantSubchain = chain.subList(
                chain.indexOf(binding), chain.lastIndex
            )
            if (relevantSubchain.any {
                    it is ProviderBindingNode ||
                            it.type.classifier.fqName.asString().startsWith("kotlin.Function")
                }) return
            error(
                "Circular dependency ${relevantSubchain.map { it.type.render() }} already contains ${binding.type.render()} $chain"
            )
        }
        chain.push(binding)
        binding
            .dependencies
            .forEach { check(it) }
        (binding as? ChildImplBindingNode)?.childComponentImpl?.initialize()
        chain.pop()
    }

    private fun check(request: BindingRequest) {
        check(getBinding(request))
    }

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = getBindingOrNull(request)
        if (binding != null) return binding

        if (request.type.isMarkedNullable) {
            binding = NullBindingNode(request.type, owner)
            bindingsByRequest[request.type] = binding
            resolvedBindings += binding
            return binding
        }

        error(
            buildString {
                var indendation = ""
                fun indent() {
                    indendation = "$indendation    "
                }
                appendLine("No binding found for '${request.type.render()}' in '${componentType.render()}':")
                appendLine("${request.origin.orUnknown()} requires '${request.type.render()}'")
                chain.forEach {
                    appendLine("chain $it" + it?.origin.orUnknown())
                }
                /*chain.forEachIndexed { index, binding ->
                    appendLine("${indendation}${binding.origin.orUnknown()} requires binding '${binding.type.render()}'")
                    indent()
                }*/
            }
        )
    }

    private fun getBindingOrNull(request: BindingRequest): BindingNode? {
        fun List<BindingNode>.mostSpecificOrFail(bindingType: String): BindingNode? {
            return if (size > 1) {
                getExact(request.type)
                    ?: error(
                        "Multiple $bindingType bindings found for '${request.type.render()}' at:\n${
                            joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                        }"
                    )
            } else {
                singleOrNull()
            }
        }

        val existingBindings = resolvedBindings
            .filter { request.type.isAssignable(it.type) }

        var binding = existingBindings.singleOrNull() ?: existingBindings.getExact(request.type)
        binding?.let {
            bindingsByRequest[request.type] = it
            return it
        }

        val explicitBindings = getExplicitBindingsForType(request)
        binding = explicitBindings.mostSpecificOrFail("explicit")
        binding?.let {
            bindingsByRequest[request.type] = it
            resolvedBindings += it
            return it
        }

        val (implicitInternalUserBindings, externalImplicitUserBindings) = getImplicitUserBindingsForType(request)
            .partition { !it.isExternal }

        binding = implicitInternalUserBindings.mostSpecificOrFail("internal implicit")
        binding?.let {
            bindingsByRequest[request.type] = it
            resolvedBindings += it
            return it
        }

        binding = externalImplicitUserBindings.mostSpecificOrFail("external implicit")
        binding?.let {
            bindingsByRequest[request.type] = it
            resolvedBindings += it
            return it
        }

        val implicitFrameworkBindings = getImplicitFrameworkBindingsForType(request)
        binding = implicitFrameworkBindings.mostSpecificOrFail("")
        binding?.let {
            bindingsByRequest[request.type] = it
            resolvedBindings += it
            return it
        }

        parent?.getBindingOrNull(request)?.let {
            bindingsByRequest[request.type] = it
            resolvedBindings += it
            return it
        }

        return null
    }

    private fun getExplicitBindingsForType(
        request: BindingRequest
    ): List<BindingNode> = buildList<BindingNode> {
        this += moduleBindingCallables
            .filter { request.type.isAssignable(it.callable.type) }
            .map { (callable, receiver) ->
                val substitutionMap = request.type.getSubstitutionMap(callable.type)
                CallableBindingNode(
                    type = request.type,
                    rawType = callable.type,
                    owner = owner,
                    dependencies = callable.valueParameters
                        .filterNot { it.isAssisted }
                        .map {
                            BindingRequest(
                                it.type.substitute(substitutionMap),
                                callable.fqName.child(it.name),
                                callable.isInline && (it.type.isFunction || it.type.isSuspendFunction)
                            )
                        },
                    valueParameters = callable.valueParameters.map {
                        it.copy(
                            type = it.type.substitute(substitutionMap)
                        )
                    },
                    origin = callable.fqName,
                    targetComponent = callable.targetComponent,
                    receiver = receiver,
                    callable = callable,
                    isExternal = callable.isExternal,
                    cacheable = callable.isEager || callable.valueParameters.any {
                        it.isAssisted
                    }
                )
            }
    }

    private fun getImplicitUserBindingsForType(request: BindingRequest): List<BindingNode> {
        return declarationStore.bindingsForType(request.type)
            .filter { it.targetComponent == null || it.targetComponent == owner.componentType }
            .map { callable ->
                val substitutionMap = request.type.getSubstitutionMap(callable.type)
                CallableBindingNode(
                    type = request.type,
                    rawType = callable.type,
                    owner = owner,
                    dependencies = callable.valueParameters
                        .filterNot { it.isAssisted }
                        .map {
                            BindingRequest(
                                it.type.substitute(substitutionMap),
                                callable.fqName.child(it.name),
                                callable.isInline && (it.type.isFunction || it.type.isSuspendFunction)
                            )
                        },
                    valueParameters = callable.valueParameters.map {
                        it.copy(
                            type = it.type.substitute(substitutionMap)
                        )
                    },
                    origin = callable.fqName,
                    targetComponent = callable.targetComponent,
                    receiver = null,
                    callable = callable,
                    isExternal = callable.isExternal,
                    cacheable = callable.isEager || callable.valueParameters.any {
                        it.isAssisted
                    }
                )
            }
    }

    private fun getBindingsForStarProjectedType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        val componentRequestForType = owner.allRequests
            .singleOrNull { request.type.isAssignable(it.type) }
        if (componentRequestForType != null) {
            DelegateBindingNode(
                componentRequestForType.type,
                owner,
                BindingRequest(componentRequestForType.type,
                    componentRequestForType.fqName, false)
            )
        } else {
            val substitutionMap = request.type.getSubstitutionMap(binding.rawType)
            DelegateBindingNode(
                request.type,
                owner,
                BindingRequest(
                    binding.rawType.substitute(substitutionMap),
                    request.origin,
                    request.isInline
                )
            )
        }
    }

    private fun getImplicitFrameworkBindingsForType(
        request: BindingRequest
    ): List<BindingNode> = buildList<BindingNode> {
        if (request.type == componentType) {
            this += SelfBindingNode(
                type = request.type,
                component = owner
            )
        }

        if (request.type.isFunction && request.type.typeArguments.last().let {
                it.isChildComponent || it.isMergeChildComponent
            }) {
            // todo check if the arguments match the constructor arguments of the child component
            val childComponentType = request.type.typeArguments.last()
            val existingComponents = mutableSetOf<TypeRef>()
            var currentComponent: ComponentImpl? = owner
            while (currentComponent != null) {
                existingComponents += currentComponent.componentType
                currentComponent = currentComponent.parent
            }
            if (childComponentType !in existingComponents) {
                val componentImpl = componentImplFactory(
                    childComponentType,
                    owner.contextTreeNameProvider(
                        childComponentType.classifier.fqName.shortName().asString()
                    ).asNameId(),
                    owner
                )
                this += ChildImplBindingNode(
                    type = request.type,
                    owner = owner,
                    origin = null,
                    childComponentImpl = componentImpl
                )
            }
        }

        if ((request.type.isFunction || request.type.isSuspendFunction) && request.type.typeArguments.size == 1 &&
            request.type.typeArguments.last().let {
                !it.isChildComponent && !it.isMergeChildComponent
            }) {
            this += ProviderBindingNode(
                type = request.type,
                owner = owner,
                dependencies = listOf(
                    BindingRequest(
                        request.type.typeArguments.single(),
                        FqName.ROOT, // todo
                        request.isInline
                    )
                ),
                FqName.ROOT // todo
            )
        }

        this += collections.getNodes(request)
    }

    private fun List<BindingNode>.getExact(requested: TypeRef): BindingNode? =
        singleOrNull { it.rawType == requested }

    private val ComponentImpl.allRequests: List<Callable>
        get() = requests + (parent?.allRequests ?: emptyList())
}

private fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"

@Binding
class BindingCollections(
    private val declarationStore: DeclarationStore,
    private val owner: @Assisted ComponentImpl,
    private val parent: @Assisted BindingCollections?,
) {

    private val thisMapEntries = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()
    private val thisSetElements = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()

    fun addMapEntries(entries: CallableWithReceiver) {
        thisMapEntries.getOrPut(entries.callable.type) { mutableListOf() } += entries
    }

    fun addSetElements(elements: CallableWithReceiver) {
        thisSetElements.getOrPut(elements.callable.type) { mutableListOf() } += elements
    }

    private val mapEntriesByType = mutableMapOf<TypeRef, List<CallableWithReceiver>>()
    private fun getMapEntries(type: TypeRef): List<CallableWithReceiver> {
        return mapEntriesByType.getOrPut(type) {
            (parent?.getMapEntries(type) ?: emptyList()) +
                    (if (parent == null) declarationStore.mapEntriesByType(type)
                        .map { CallableWithReceiver(it, null, emptyMap()) }
                    else emptyList()) +
                    (thisMapEntries[type] ?: emptyList())
        }
    }

    private val setElementsByType = mutableMapOf<TypeRef, List<CallableWithReceiver>>()
    private fun getSetElements(type: TypeRef): List<CallableWithReceiver> {
        return setElementsByType.getOrPut(type) {
            (parent?.getSetElements(type) ?: emptyList()) +
                    (if (parent == null) declarationStore.setElementsByType(type)
                        .map { CallableWithReceiver(it, null, emptyMap()) }
                    else emptyList()) +
                (thisSetElements[type] ?: emptyList())
        }
    }

    fun getNodes(request: BindingRequest): List<BindingNode> {
        return listOfNotNull(
            getMapEntries(request.type)
                .takeIf { it.isNotEmpty() }
                ?.let { entries ->
                    MapBindingNode(
                        type = request.type,
                        owner = owner,
                        dependencies = entries.flatMap { (entry, _, substitutionMap) ->
                            entry.valueParameters
                                .filterNot { it.isAssisted }
                                .map {
                                    BindingRequest(
                                        it.type.substitute(substitutionMap),
                                        entry.fqName.child(it.name),
                                        false
                                    )
                                }
                        },
                        entries = entries
                    )
                },
            getSetElements(request.type)
                .takeIf { it.isNotEmpty() }
                ?.let { elements ->
                    SetBindingNode(
                        type = request.type,
                        owner = owner,
                        dependencies = elements.flatMap { (element, _, substitutionMap) ->
                            element.valueParameters
                                .filterNot { it.isAssisted }
                                .map {
                                    BindingRequest(
                                        it.type.substitute(substitutionMap),
                                        element.fqName.child(it.name),
                                        false
                                    )
                                }
                        },
                        elements = elements
                    )
                }
        )
    }
}
