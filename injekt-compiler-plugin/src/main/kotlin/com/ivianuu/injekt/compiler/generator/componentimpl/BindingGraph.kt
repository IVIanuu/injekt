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
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.ErrorCollector
import com.ivianuu.injekt.compiler.generator.ModuleDescriptor
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.ValueParameterRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.callableKind
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.isAssignableTo
import com.ivianuu.injekt.compiler.generator.isSubTypeOf
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.replaceTypeParametersWithStars
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.substituteStars
import com.ivianuu.injekt.compiler.generator.toClassifierRef
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import com.ivianuu.injekt.compiler.generator.unsafeLazy
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


@Binding class BindingGraph(
    private val owner: ComponentImpl,
    private val declarationStore: DeclarationStore,
    private val componentImplFactory: (
        ComponentType?,
        Callable?,
        ScopeType?,
        ComponentFactoryType,
        Name,
        List<TypeRef>,
        Boolean,
        @Parent ComponentImpl?,
    ) -> ComponentImpl,
    private val errorCollector: ErrorCollector,
    private val moduleDescriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor,
) {

    private val parent = owner.parent?.graph

    private val parentsTopDown: List<BindingGraph> by unsafeLazy {
        parentsBottomUp.reversed()
    }

    private val parentsBottomUp: List<BindingGraph> by unsafeLazy {
        buildList<BindingGraph> {
            var current = parent
            while (current != null) {
                this += current
                current = current.parent
            }
        }
    }

    private val explicitBindings = mutableListOf<Callable>()
    private val implicitBindings = mutableListOf<Callable>()
    private val explicitInterceptors = mutableListOf<InterceptorNode>()
    private val implicitInterceptors = mutableListOf<InterceptorNode>()
    private val explicitMapEntries = mutableMapOf<TypeRef, MutableList<Callable>>()
    private val implicitMapEntries = mutableMapOf<TypeRef, MutableList<Callable>>()
    private val explicitSetElements = mutableMapOf<TypeRef, MutableList<Callable>>()
    private val implicitSetElements = mutableMapOf<TypeRef, MutableList<Callable>>()

    val resolvedBindings = mutableMapOf<TypeRef, BindingNode>()

    private val chain = mutableListOf<BindingRequest>()
    private var locked = false

    private val mapType = moduleDescriptor.builtIns.map.toClassifierRef().defaultType
    private val setType = moduleDescriptor.builtIns.set.toClassifierRef().defaultType

    private val collectedModules = mutableSetOf<TypeRef>()

    init {
        explicitBindings += owner.inputTypes
            .map {
                Callable(
                    FqName.ROOT,
                    FqName.ROOT,
                    "i_${it.uniqueTypeName()}".asNameId(),
                    it,
                    it,
                    emptyList(),
                    listOf(
                        ValueParameterRef(
                            owner.type,
                            owner.type,
                            ValueParameterRef.ParameterKind.DISPATCH_RECEIVER,
                            "\$dispatchReceiver".asNameId(),
                            false,
                            null
                        )
                    ),
                    null,
                    false,
                    false,
                    false,
                    null,
                    false,
                    Callable.CallableKind.DEFAULT,
                    false,
                    Modality.FINAL
                )
            }

        owner.inputTypes
            .map { it to declarationStore.moduleForType(it) }
            .forEach { (origin, explicitModule) ->
                explicitModule.collectContributions(
                    path = listOf(origin.classifier.fqName),
                    addBinding = { explicitBindings += it },
                    addInterceptor = { explicitInterceptors += it },
                    addMapEntries = {
                        explicitMapEntries.getOrPut(it.type) { mutableListOf() } += it
                    },
                    addSetElements = {
                        explicitSetElements.getOrPut(it.type) { mutableListOf() } += it
                    }
                )
            }

        declarationStore.allModules
            .filter { it.targetComponent.checkScope() }
            .map { it to declarationStore.moduleForType(it.type) }
            .forEach { (origin, implicitModule) ->
                implicitModule.collectContributions(
                    path = listOf(origin.fqName),
                    addBinding = { implicitBindings += it },
                    addInterceptor = { implicitInterceptors += it },
                    addMapEntries = {
                        implicitMapEntries.getOrPut(it.type) { mutableListOf() } += it
                    },
                    addSetElements = {
                        implicitSetElements.getOrPut(it.type) { mutableListOf() } += it
                    }
                )
            }
    }

    private fun ModuleDescriptor.collectContributions(
        path: List<Any>,
        addBinding: (Callable) -> Unit,
        addInterceptor: (InterceptorNode) -> Unit,
        addMapEntries: (Callable) -> Unit,
        addSetElements: (Callable) -> Unit,
    ) {
        if (!type.allTypes.any { it.isFunction || it.isSuspendFunction }) {
            if (type in collectedModules) return
            collectedModules += type
        }
        for (callable in callables) {
            if (callable.contributionKind == null) continue
            when (callable.contributionKind!!) {
                Callable.ContributionKind.BINDING -> addBinding(callable)
                Callable.ContributionKind.INTERCEPTOR -> addInterceptor(
                    InterceptorNode(
                        callable,
                        callable.getDependencies(callable.type, true)
                    )
                )
                Callable.ContributionKind.MAP_ENTRIES -> addMapEntries(callable)
                Callable.ContributionKind.SET_ELEMENTS -> addSetElements(callable)
                Callable.ContributionKind.MODULE -> {
                    val isFunction =
                        callable.type.allTypes.any { it.isFunction || it.isSuspendFunction }
                    if (isFunction || callable.type !in collectedModules) {
                        val nextPath = path + callable.fqName
                        val (nextCallable, nextModule) = if (isFunction) {
                            val nextCallable =
                                callable.copy(type = callable.type.copy(path = nextPath))
                            nextCallable to declarationStore.moduleForType(nextCallable.type)
                        } else {
                            callable to declarationStore.moduleForType(callable.type)
                        }
                        addBinding(nextCallable)
                        nextModule.collectContributions(
                            nextPath,
                            addBinding,
                            addInterceptor,
                            addMapEntries,
                            addSetElements
                        )
                    } else Unit
                }
            }.let {}
        }
    }

    fun checkRequests(requests: List<BindingRequest>) {
        requests.forEach { check(it) }
        requests.forEach { request ->
            val binding = getBinding(request)
            if (binding.callableKind != Callable.CallableKind.DEFAULT &&
                binding.callableKind != request.callableKind
            ) {
                errorCollector.add("Call context mismatch. '${request.origin}' is a ${request.callableKind.name} callable but " +
                        "dependency '${binding.origin}' is a ${binding.callableKind.name} callable.")
            }
        }
        postProcess()
        locked = true
    }

    private fun check(binding: BindingNode) {
        // recursive check all dependencies
        binding
            .dependencies
            .forEach { check(it) }
        (binding as? ChildComponentBindingNode)?.childComponent?.initialize()
        (binding as? AssistedBindingNode)?.childComponent?.initialize()

        // check that all calling contexts are the same
        if (binding.callableKind == Callable.CallableKind.DEFAULT) {
            val dependenciesByCallableKind = binding.dependencies
                .map { it to getBinding(it) }
                .filter { it.first.callableKind != it.second.callableKind }
                .filter { it.second.callableKind != Callable.CallableKind.DEFAULT }
                .groupBy { it.second.callableKind }
            if (dependenciesByCallableKind.size > 1) {
                errorCollector.add("Dependencies call context mismatch. Dependencies of '${binding.origin}' have different call contexts\n" +
                        binding.dependencies.joinToString("\n") { dependency ->
                            val dependencyBinding = getBinding(dependency)
                            "${dependency.origin} -> '${dependencyBinding.origin}' = ${dependencyBinding.callableKind.name}"
                        }
                )
            }
        }

        // adapt call context of dependencies or throw if not possible
        binding.dependencies
            .map { it to getBinding(it) }
            .filter { it.second.callableKind != Callable.CallableKind.DEFAULT }
            .filter { it.first.callableKind != it.second.callableKind }
            .forEach { (request, dependency) ->
                if (request.callableKind != Callable.CallableKind.DEFAULT &&
                    request.callableKind != dependency.callableKind
                ) {
                    errorCollector.add("Call context mismatch. '${request.origin}' is a ${request.callableKind.name} callable but " +
                            "dependency '${dependency.origin}' is a ${dependency.callableKind.name} callable.")
                } else {
                    binding.callableKind = dependency.callableKind
                }
            }

        binding.refineType(binding.dependencies.map { getBinding(it) })

        binding.interceptors = if (binding !is SelfBindingNode &&
            (binding !is CallableBindingNode ||
                    binding.callable.contributionKind != Callable.ContributionKind.MODULE)
        )
            getInterceptorsForType(binding.type, binding.callableKind)
        else emptyList()

        binding.interceptors
            .forEach { interceptor ->
                chain.push(
                    BindingRequest(
                        interceptor.callable.type,
                        interceptor.callable.fqName,
                        true,
                        interceptor.callable.callableKind,
                        false,
                        false
                    )
                )
                interceptor.dependencies.forEach { dependency ->
                    check(dependency)
                }
                chain.pop()
            }

        // we mark all scoped dependencies of the binding as eager
        // to avoid to create additional properties for their instances
        markDependenciesAndInterceptorsAsEager(binding)
    }

    private fun markDependenciesAndInterceptorsAsEager(binding: BindingNode) {
        if (binding.scoped && binding.eager && binding.owner == owner) {
            markScopedRequestsAsEager(binding.dependencies)
        }
        // interceptors will be eagerly created so
        // mark each of their dependencies as eager
        markScopedRequestsAsEager(
            binding.interceptors
                .flatMap { it.dependencies }
        )
    }

    private fun markScopedRequestsAsEager(requests: List<BindingRequest>) {
        requests
            .filterNot { it.lazy || it.type == owner.componentType }
            .map { getBinding(it) }
            .filter { it.scoped && !it.eager && it.owner == owner }
            .forEach {
                it.eager = true
                markDependenciesAndInterceptorsAsEager(it)
            }
    }

    private fun check(request: BindingRequest) {
        if (request in chain) {
            val cycleChain = chain.subList(
                chain.indexOf(request), chain.size
            )
            if (request.lazy || !request.required || request.type.isMarkedNullable ||
                request.type == owner.componentType ||
                cycleChain.any {
                    it.lazy || !request.required || request.type.isMarkedNullable ||
                            request.type == owner.componentType
                }
            ) return

            val cycleOriginRequest = chain[chain.indexOf(request) - 1]

            errorCollector.add(
                buildString {
                    appendLine("Circular dependency:")
                    (cycleChain.reversed() + cycleOriginRequest).forEachIndexed { index, request ->
                        append("'${request.type.render()}' ")
                        if (index == cycleChain.size) appendLine("is provided at")
                        else appendLine("is injected at")
                        appendLine("    '${request.origin}'")
                    }
                }
            )
        }
        chain.push(request)
        val binding = getBinding(request)
        if (owner.isAssisted &&
            request.type == owner.requests.singleOrNull()?.type &&
            binding is CallableBindingNode &&
            binding.eager
        ) {
            errorCollector.add("Cannot perform assisted injection on a eager binding $request ${binding.callable.fqName}")
        }
        binding.owner.graph.check(binding)
        chain.pop()
    }

    // temporary function because it's not possible to properly inline '*' star types
    // with our current binding resolution algorithm.
    // we collect all duplicated bindings and rewrite the graph to point to a single binding
    private fun postProcess() {
        class MergeBindingGroup(
            val type: TypeRef,
            val bindingToUse: BindingNode,
        ) {
            val keysToReplace = mutableListOf<TypeRef>()
        }

        val bindingGroups = mutableListOf<MergeBindingGroup>()
        resolvedBindings.forEach { (key, binding) ->
            val bindingGroup = bindingGroups.singleOrNull { it.type == binding.type }
            if (bindingGroup != null) {
                bindingGroup.keysToReplace += key
                // The components aren't needed if we get delegate to another binding
                if (binding is AssistedBindingNode)
                    owner.children -= binding.childComponent
                if (binding is ChildComponentBindingNode)
                    owner.children -= binding.childComponent
            } else {
                bindingGroups += MergeBindingGroup(key, binding)
                    .also { it.keysToReplace += binding.type }
            }
        }

        bindingGroups.forEach { bindingGroup ->
            bindingGroup.keysToReplace.forEach { key ->
                resolvedBindings[key] = bindingGroup.bindingToUse
            }
        }
    }

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = getBindingOrNull(request)
        if (binding != null) return binding

        if (request.type.isMarkedNullable || !request.required) {
            binding = MissingBindingNode(request.type, owner)
            resolvedBindings[request.type] = binding
            return binding
        }

        errorCollector.add(
            buildString {
                appendLine("No binding found for '${request.type.render()}':")
                chain.reversed().forEachIndexed { index, request ->
                    append("'${request.type.render()}' ")
                    if (index == chain.lastIndex) appendLine("is provided at")
                    else appendLine("is injected at")
                    appendLine("    '${request.origin}'")
                }
            }
        )
    }

    private fun getBindingOrNull(request: BindingRequest): BindingNode? {
        var binding = resolvedBindings[request.type]
        if (binding != null) return binding

        check(!locked) {
            "Cannot create request new bindings in ${owner.nonAssistedComponent.componentType} for $request " +
                    "existing ${resolvedBindings.keys}"
        }

        if (request.forObjectCall) {
            binding = declarationStore.callableForDescriptor(
                declarationStore.classDescriptorForFqName(request.type.classifier.fqName)
                    .unsubstitutedPrimaryConstructor!!
            ).toCallableBindingNode(request)
            resolvedBindings[request.type] = binding
            return binding
        }

        fun List<BindingNode>.mostSpecificOrFail(bindingKind: String): BindingNode? {
            if (isEmpty()) return null

            if (size == 1) return single()

            val exact = filter { it.rawType == request.type }
            if (exact.size == 1) return exact.single()

            errorCollector.add(
                "Multiple $bindingKind bindings found for '${request.type.render()}' required by '${request.origin}' at:\n${
                    joinToString("\n") { "    '${it.origin}' $it" }
                }"
            )
        }

        val explicitBindings = getExplicitBindingsForType(request, false)
        binding = explicitBindings.mostSpecificOrFail("explicit")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        parentsBottomUp.forEach { parent ->
            val explicitParentBindings = getExplicitParentBindingsForType(parent, request)
            explicitParentBindings.singleOrNull()?.let {
                resolvedBindings[request.type] = it
                return it
            }
        }

        val implicitUserBindings = getImplicitUserBindingsForType(request, false)

        binding = implicitUserBindings.mostSpecificOrFail("implicit")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        val explicitDefaultBindings = getExplicitBindingsForType(request, true)
        binding = explicitDefaultBindings.mostSpecificOrFail("explicit default")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        val implicitDefaultUserBindings =
            getImplicitUserBindingsForType(request, true)

        binding = implicitDefaultUserBindings.mostSpecificOrFail("implicit default")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        val implicitFrameworkBindings = getImplicitFrameworkBindingsForType(request)
        binding = implicitFrameworkBindings.mostSpecificOrFail("implicit framework")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        parent?.getBindingOrNull(request)?.let {
            resolvedBindings[request.type] = it
            return it
        }

        return null
    }

    private fun getExplicitBindingsForType(
        request: BindingRequest,
        default: Boolean,
    ): List<BindingNode> = buildList<BindingNode> {
        this += explicitBindings
            .filter { it.default == default }
            .filter { it.type.isAssignableTo(request.type) }
            .map { it.toCallableBindingNode(request) }
    }

    private fun getExplicitParentBindingsForType(
        parent: BindingGraph,
        request: BindingRequest,
    ): List<BindingNode> = buildList<BindingNode> {
        this += parent.explicitBindings
            .filter { it.targetComponent.checkScope() }
            .filter { it.type.isAssignableTo(request.type) }
            .map { it.toCallableBindingNode(request) }
    }

    private fun getImplicitUserBindingsForType(
        request: BindingRequest,
        default: Boolean,
    ): List<BindingNode> = buildList<BindingNode> {
        this += declarationStore.bindingsForType(request.type)
            .filter { it.default == default }
            .filter { it.targetComponent.checkScope() }
            .map { it.toCallableBindingNode(request) }
        this += implicitBindings
            .filter { it.default == default }
            .filter { it.type.isAssignableTo(request.type) }
            .map { it.toCallableBindingNode(request) }
    }

    private fun getImplicitFrameworkBindingsForType(request: BindingRequest): List<BindingNode> =
        buildList<BindingNode> {
            if (request.type == owner.componentType ||
                request.type == owner.type
            ) {
                this += SelfBindingNode(
                    type = SimpleTypeRef(
                        classifier = ClassifierRef(
                            FqName(owner.name.asString())
                        )
                    ),
                    component = owner
                )
            }

            if (request.type.isFunction && request.type.typeArguments.last().let {
                    it.classifier.isComponent || it.classifier.isMergeComponent
                }) {
                // todo check if the arguments match the constructor arguments of the child component
                val childComponentType = request.type.typeArguments.last()
                val childComponentConstructor =
                    declarationStore.constructorForComponent(childComponentType)
                val inputTypes = request.type.typeArguments.dropLast(1)
                    .filter { inputType ->
                        childComponentConstructor == null ||
                                childComponentConstructor.valueParameters.none {
                                    it.type == inputType
                                }
                    }
                val existingComponents = mutableSetOf<TypeRef>()
                var currentComponent: ComponentImpl? = owner
                while (currentComponent != null) {
                    if (currentComponent.componentType != null)
                        existingComponents += currentComponent.componentType!!
                    currentComponent = currentComponent.parent
                }
                if (childComponentType !in existingComponents) {
                    val componentImpl = componentImplFactory(
                        childComponentType,
                        null,
                        childComponentType.classifier.targetScope,
                        request.type,
                        owner.contextTreeNameProvider(
                            "${owner.rootComponent.name}_${
                                childComponentType.classifier.fqName.shortName().asString()
                            }Impl"
                        ).asNameId(),
                        inputTypes,
                        false,
                        owner
                    )
                    this += ChildComponentBindingNode(
                        type = request.type,
                        owner = owner,
                        origin = null,
                        childComponent = componentImpl
                    )
                }
            }

            if ((request.type.isFunction || request.type.isSuspendFunction) && request.type.typeArguments.size == 1 &&
                request.type.typeArguments.last().let {
                    !it.classifier.isComponent && !it.classifier.isMergeComponent
                }
            ) {
                this += ProviderBindingNode(
                    type = request.type,
                    owner = owner,
                    dependencies = listOf(
                        BindingRequest(
                            type = request.type.typeArguments.single(),
                            origin = request.origin,
                            required = true,
                            callableKind = request.type.callableKind,
                            lazy = true,
                            forObjectCall = false
                        )
                    ),
                    origin = request.origin
                )
            }

            if ((request.type.isFunction || request.type.isSuspendFunction) &&
                request.type.typeArguments.last().let {
                    !it.classifier.isComponent && !it.classifier.isMergeComponent
                }
            ) {
                val factoryExists = generateSequence(owner) { it.parent }
                    .filter { it.componentFactoryType == request.type }
                    .any()
                val inputTypes = request.type.typeArguments.dropLast(1).distinct()
                if (!factoryExists && inputTypes.isNotEmpty()) {
                    val returnType = request.type.typeArguments.last()

                    val bindingCallable = Callable(
                        packageFqName = FqName.ROOT,
                        fqName = request.origin,
                        name = "invoke".asNameId(),
                        type = returnType,
                        typeParameters = emptyList(),
                        valueParameters = emptyList(),
                        targetComponent = null,
                        scoped = false,
                        eager = false,
                        default = false,
                        contributionKind = null,
                        isCall = true,
                        callableKind = request.type.callableKind,
                        isInline = true,
                        modality = Modality.FINAL
                    )

                    val childComponent = componentImplFactory(
                        null,
                        bindingCallable,
                        null,
                        request.type,
                        owner.contextTreeNameProvider("${owner.rootComponent.name}_AC").asNameId(),
                        inputTypes,
                        true,
                        owner
                    )
                    this += AssistedBindingNode(
                        type = request.type,
                        owner = owner,
                        childComponent = childComponent,
                        assistedTypes = inputTypes
                    )
                }
            }

            if (request.type.isSubTypeOf(mapType)) {
                var mapEntries = getMapEntriesForType(request.type, false)
                if (mapEntries.isEmpty()) {
                    mapEntries = getMapEntriesForType(request.type, true)
                }
                if (mapEntries.isNotEmpty()) {
                    val dependenciesByEntry = mapEntries.map { entry ->
                        entry to entry.getDependencies(entry.type, false)
                    }.toMap()
                    this += MapBindingNode(
                        type = request.type,
                        owner = owner,
                        dependencies = dependenciesByEntry.flatMap { it.value },
                        entries = mapEntries,
                        dependenciesByEntry = dependenciesByEntry
                    )
                }
            }

            if (request.type.isSubTypeOf(setType)) {
                var setElements = getSetElementsForType(request.type, false)
                if (setElements.isEmpty()) {
                    setElements = getSetElementsForType(request.type, true)
                }
                if (setElements.isNotEmpty()) {
                    val dependenciesByElement = setElements.map { element ->
                        element to element.getDependencies(element.type, false)
                    }.toMap()
                    this += SetBindingNode(
                        type = request.type,
                        owner = owner,
                        dependencies = dependenciesByElement.flatMap { it.value },
                        elements = setElements,
                        dependenciesByElement = dependenciesByElement
                    )
                }
            }
        }

    private fun getMapEntriesForType(type: TypeRef, default: Boolean) = buildList<Callable> {
        this += declarationStore.mapEntriesByType(type)
            .filter { it.targetComponent.checkScope() }
        parentsTopDown.forEach { parent ->
            parent.implicitMapEntries[type]
                ?.filter {
                    with(parent) {
                        it.targetComponent.checkScope()
                    }
                }
                ?.let { this += it }
        }
        implicitMapEntries[type]
            ?.filter { it.targetComponent.checkScope() }
            ?.let { this += it }
        parentsTopDown.forEach { parent ->
            parent.explicitMapEntries[type]?.let { this += it }
        }
        explicitMapEntries[type]?.let { this += it }
    }
        .filter { it.default == default }
        .map { entry ->
            entry.substitute(
                getSubstitutionMap(listOf(type to entry.type))
            )
        }

    private fun getSetElementsForType(type: TypeRef, default: Boolean) = buildSet<Callable> {
        this += declarationStore.setElementsByType(type)
            .filter { it.targetComponent.checkScope() }
        parentsTopDown.forEach { parent ->
            parent.implicitSetElements[type]
                ?.filter {
                    with(parent) {
                        it.targetComponent.checkScope()
                    }
                }
                ?.let { this += it }
        }
        implicitSetElements[type]
            ?.filter { it.targetComponent.checkScope() }
            ?.let { this += it }
        parentsTopDown.forEach { parent ->
            parent.explicitSetElements[type]?.let { this += it }
        }
        explicitSetElements[type]?.let { this += it }
    }
        .filter { it.default == default }
        .map { element ->
            element.substitute(
                getSubstitutionMap(listOf(type to element.type))
            )
        }

    private fun TypeRef?.checkScope(): Boolean =
        this == null || (owner.nonAssistedComponent.scopeType != null &&
                this.isAssignableTo(owner.nonAssistedComponent.scopeType!!))

    private fun Callable.getDependencies(
        type: TypeRef,
        isInterceptor: Boolean,
    ): List<BindingRequest> {
        val substitutionMap = getSubstitutionMap(listOf(type to this.type))
        return valueParameters
            .map { it.toBindingRequest(this, substitutionMap) }
            .filter { !isInterceptor || it.type != this.type.substitute(substitutionMap) }
    }

    private fun getInterceptorsForType(
        type: TypeRef,
        callableKind: Callable.CallableKind,
    ): List<InterceptorNode> {
        val providerType = when (callableKind) {
            Callable.CallableKind.DEFAULT -> moduleDescriptor.builtIns.getFunction(0)
                .toClassifierRef().defaultType.typeWith(listOf(type))
            Callable.CallableKind.SUSPEND -> moduleDescriptor.builtIns.getSuspendFunction(0)
                .toClassifierRef().defaultType.typeWith(listOf(type))
            Callable.CallableKind.COMPOSABLE -> moduleDescriptor.builtIns.getFunction(0)
                .toClassifierRef().defaultType.typeWith(listOf(type))
                .copy(isComposable = true)
        }
        return getInterceptorsForType(providerType)
            .map { interceptor ->
                val substitutionMap =
                    getSubstitutionMap(listOf(providerType to interceptor.callable.type))
                val finalCallable = interceptor.callable.substitute(substitutionMap)
                interceptor.copy(
                    callable = finalCallable,
                    dependencies = finalCallable.getDependencies(type, true)
                )
            }
    }

    private fun getInterceptorsForType(providerType: TypeRef): List<InterceptorNode> =
        buildList<InterceptorNode> {
            this += explicitInterceptors
                .filter { it.callable.type.isAssignableTo(providerType) }
                .filter { it.callable.targetComponent.checkScope() }
                .filter {
                    providerType.typeArguments.last() != it.callable.valueParameters
                        .firstOrNull { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                        ?.type
                }
            this += parentsBottomUp.flatMap { parent ->
                parent.explicitInterceptors
                    .filter { it.callable.type.isAssignableTo(providerType) }
                    .filter {
                        with(parent) {
                            it.callable.targetComponent.checkScope()
                        }
                    }
                    .filter {
                        providerType.typeArguments.last() != it.callable.valueParameters
                            .firstOrNull { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                            ?.type
                    }
            }
            this += implicitInterceptors
                .filter { it.callable.type.isAssignableTo(providerType) }
                .filter { it.callable.targetComponent.checkScope() }
                .filter {
                    providerType.typeArguments.last() != it.callable.valueParameters
                        .firstOrNull { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                        ?.type
                }
            this += parentsBottomUp.flatMap { parent ->
                parent.implicitInterceptors
                    .filter { it.callable.type.isAssignableTo(providerType) }
                    .filter {
                        with(parent) {
                            it.callable.targetComponent.checkScope()
                        }
                    }
                    .filter {
                        providerType.typeArguments.last() != it.callable.valueParameters
                            .firstOrNull { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                            ?.type
                    }
            }
            this += declarationStore.interceptorsByType(providerType)
                .filter { it.targetComponent.checkScope() }
                .map { interceptor ->
                    InterceptorNode(
                        interceptor,
                        interceptor.getDependencies(interceptor.type, true)
                    )
                }
                .filter {
                    providerType.typeArguments.last() != it.callable.valueParameters
                        .firstOrNull { it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER }
                        ?.type
                }
        }.distinct()

    private fun Callable.toCallableBindingNode(request: BindingRequest): CallableBindingNode {
        val substitutionMap = getSubstitutionMap(listOf(request.type to type))
        val finalCallable = substitute(substitutionMap)
        return CallableBindingNode(
            type = request.type.substituteStars(finalCallable.type)
                .makeNonNullIfPossible(finalCallable),
            rawType = finalCallable.originalType,
            owner = owner,
            dependencies = finalCallable.getDependencies(request.type, false),
            callable = finalCallable
        )
    }

    private fun ValueParameterRef.toBindingRequest(
        callable: Callable,
        substitutionMap: Map<ClassifierRef, TypeRef>,
    ): BindingRequest = BindingRequest(
        type = type.substitute(substitutionMap)
            .replaceTypeParametersWithStars(),
        origin = callable.fqName.child(name),
        required = !hasDefault,
        callableKind = callable.callableKind,
        forObjectCall = parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER &&
                type.classifier.isObject,
        lazy = false
    )

    private fun TypeRef.makeNonNullIfPossible(callable: Callable): TypeRef {
        if (!isMarkedNullable) return this
        if (callable.type.isMarkedNullable) return this
        return copy(isMarkedNullable = false)
    }

}

@Qualifier
@Target(AnnotationTarget.TYPE)
annotation class Parent
