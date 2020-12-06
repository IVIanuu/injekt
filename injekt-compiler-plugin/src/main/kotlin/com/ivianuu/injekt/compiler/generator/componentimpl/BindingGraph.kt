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
import com.ivianuu.injekt.compiler.generator.FunBindingDescriptor
import com.ivianuu.injekt.compiler.generator.ModuleDescriptor
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.ValueParameterRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.callableKind
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.isAssignable
import com.ivianuu.injekt.compiler.generator.isSubTypeOf
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.replaceTypeParametersWithStars
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.substituteStars
import com.ivianuu.injekt.compiler.generator.toClassifierRef
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.compiler.generator.unsafeLazy
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Binding
class BindingGraph(
    private val owner: ComponentImpl,
    private val declarationStore: DeclarationStore,
    private val componentImplFactory: (
        TypeRef,
        ComponentFactoryType,
        Name,
        List<TypeRef>,
        List<Callable>,
        @Parent ComponentImpl?,
    ) -> ComponentImpl,
    private val errorCollector: ErrorCollector,
    private val moduleDescriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor
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
        declarationStore.moduleForType(owner.componentType)
            .collectContributions(
                addBinding = { explicitBindings += it },
                addInterceptor = { explicitInterceptors += it },
                addMapEntries = {
                    explicitMapEntries.getOrPut(it.type) { mutableListOf() } += it
                },
                addSetElements = {
                    explicitSetElements.getOrPut(it.type) { mutableListOf() } += it
                }
            )

        declarationStore.allModules
            .filter { it.targetComponent.checkComponent() }
            .map { declarationStore.moduleForType(it.type) }
            .forEach { implicitModule ->
                implicitModule.collectContributions(
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
        addBinding: (Callable) -> Unit,
        addInterceptor: (InterceptorNode) -> Unit,
        addMapEntries: (Callable) -> Unit,
        addSetElements: (Callable) -> Unit
    ) {
        if (type in collectedModules) return
        collectedModules += type
        for (callable in callables) {
            if (callable.contributionKind == null) continue
            when (callable.contributionKind) {
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
                    if (callable.type !in collectedModules) {
                        addBinding(callable)
                        declarationStore.moduleForType(callable.type)
                            .collectContributions(addBinding, addInterceptor, addMapEntries, addSetElements)
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
                binding.callableKind != request.callableKind) {
                errorCollector.add("Call context mismatch. '${request.origin.orUnknown()}' is a ${request.callableKind.name} callable but " +
                        "dependency '${binding.origin.orUnknown()}' is a ${binding.callableKind.name} callable.")
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
                errorCollector.add("Dependencies call context mismatch. Dependencies of '${binding.origin.orUnknown()}' have different call contexts\n" +
                        binding.dependencies.joinToString("\n") { dependency ->
                            val dependencyBinding = getBinding(dependency)
                            "${dependency.origin} -> '${dependencyBinding.origin.orUnknown()}' = ${dependencyBinding.callableKind.name}"
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
                    request.callableKind != dependency.callableKind) {
                    errorCollector.add("Call context mismatch. '${request.origin.orUnknown()}' is a ${request.callableKind.name} callable but " +
                            "dependency '${dependency.origin.orUnknown()}' is a ${dependency.callableKind.name} callable.")
                } else {
                    binding.callableKind = dependency.callableKind
                }
            }

        binding.refineType(binding.dependencies.map { getBinding(it) })

        // todo callable interceptors
        binding.interceptors = if (binding !is SelfBindingNode)
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
        if (binding.scoped && binding.eager && binding.owner == owner)
            makeAllScopedDependenciesEager(binding)
    }

    private fun makeAllScopedDependenciesEager(binding: BindingNode) {
        binding
            .dependencies
            .filterNot { it.lazy || it.type == owner.componentType }
            .map { getBinding(it) }
            .filter { it.scoped && !it.eager && it.owner == owner }
            .forEach {
                it.eager = true
                makeAllScopedDependenciesEager(it)
            }
    }

    private fun check(request: BindingRequest) {
        if (request in chain) {
            val relevantSubchain = chain.subList(
                chain.indexOf(request), chain.size
            )
            if (request.lazy || !request.required || request.type.isMarkedNullable ||
                request.type == owner.componentType ||
                relevantSubchain.any {
                    it.lazy || !request.required || request.type.isMarkedNullable ||
                            request.type == owner.componentType
                }) return
            errorCollector.add(
                "Circular dependency\n${relevantSubchain.joinToString("\n")} " +
                        "already contains\n$request\n\nDebug:\n${chain.joinToString("\n")}"
            )
        }
        chain.push(request)
        val binding = getBinding(request)
        if (request.type == owner.assistedRequests.singleOrNull()?.type &&
                binding is CallableBindingNode &&
                binding.eager) {
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
            val bindingToUse: BindingNode
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
                var indendation = ""
                fun indent() {
                    indendation = "$indendation    "
                }
                appendLine("No binding found for '${request.type.render()}' ${request.type} in '${owner.nonAssistedComponent.componentType.render()}':")
                appendLine("${request.origin.orUnknown()} requires '${request.type.render()}'")
                chain.forEach {
                    appendLine("chain $it" + it.origin.orUnknown())
                }
                /*chain.forEachIndexed { index, binding ->
                    appendLine("${indendation}${binding.origin.orUnknown()} requires binding '${binding.type.render()}'")
                    indent()
                }*/
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

            // todo pick most concrete

            // todo guard against stack overflow
            exact
                .singleOrNull { candidate ->
                    candidate.dependencies.all { dependency ->
                        getBindingOrNull(dependency) != null
                    }
                }
                ?.let { return it }

            singleOrNull { candidate ->
                candidate.dependencies.all { dependency ->
                    getBindingOrNull(dependency) != null
                }
            }?.let { return it }

            errorCollector.add(
                "Multiple $bindingKind bindings found for '${request.type.render()}' required by ${request.origin} at:\n${
                    joinToString("\n") { "    '${it.origin.orUnknown()}' $it" }
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

        val (implicitInternalUserBindings, externalImplicitUserBindings) = getImplicitUserBindingsForType(request, false)
            .partition { !it.isExternal }

        binding = implicitInternalUserBindings.mostSpecificOrFail("internal implicit")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        binding = externalImplicitUserBindings.mostSpecificOrFail("external implicit")
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

        val (implicitDefaultInternalUserBindings, externalDefaultImplicitUserBindings) =
            getImplicitUserBindingsForType(request, true)
                .partition { !it.isExternal }

        binding = implicitDefaultInternalUserBindings.mostSpecificOrFail("internal implicit default")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        binding = externalDefaultImplicitUserBindings.mostSpecificOrFail("external implicit default")
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
        default: Boolean
    ): List<BindingNode> = buildList<BindingNode> {
        this += owner.additionalInputTypes
            .filter { request.type.isAssignable(it) }
            .map {
                InputBindingNode(
                    type = it,
                    owner = owner
                )
            }

        this += explicitBindings
            .filter { it.default == default }
            .filter { request.type.isAssignable(it.type) }
            .map { it.toCallableBindingNode(request) }
    }

    private fun getExplicitParentBindingsForType(parent: BindingGraph, request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += parent.explicitBindings
            .filter { it.targetComponent.checkComponent() }
            .filter { request.type.isAssignable(it.type) }
            .map { it.toCallableBindingNode(request) }
    }

    private fun getImplicitUserBindingsForType(request: BindingRequest, default: Boolean): List<BindingNode> = buildList<BindingNode> {
        this += declarationStore.bindingsForType(request.type)
            .filter { it.default == default }
            .filter { it.targetComponent.checkComponent() }
            .map { it.toCallableBindingNode(request) }
        this += implicitBindings
            .filter { it.default == default }
            .filter { request.type.isAssignable(it.type) }
            .map { it.toCallableBindingNode(request) }
    }

    private fun getImplicitFrameworkBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        if (request.type == owner.componentType) {
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
                it.isChildComponent || it.isMergeChildComponent
            }) {
            // todo check if the arguments match the constructor arguments of the child component
            val childComponentType = request.type.typeArguments.last()
            val childComponentConstructor = declarationStore.constructorForComponent(childComponentType)
            val additionalInputTypes = request.type.typeArguments.dropLast(1)
                .filter { inputType ->
                    childComponentConstructor == null ||
                            childComponentConstructor.valueParameters.none {
                                it.type == inputType
                            }
                }
            val existingComponents = mutableSetOf<TypeRef>()
            var currentComponent: ComponentImpl? = owner
            while (currentComponent != null) {
                existingComponents += currentComponent.componentType
                currentComponent = currentComponent.parent
            }
            if (childComponentType !in existingComponents) {
                val componentImpl = componentImplFactory(
                    childComponentType,
                    request.type,
                    owner.contextTreeNameProvider(
                        "${owner.rootComponent.name}_${childComponentType.classifier.fqName.shortName().asString()}Impl"
                    ).asNameId(),
                    additionalInputTypes,
                    emptyList(),
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
                !it.isChildComponent && !it.isMergeChildComponent
            }) {
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

        this += declarationStore.funBindingsForType(request.type)
            .filter { it.callable.targetComponent.checkComponent() }
            .map { it.toFunBindingNode(request) }

        if ((request.type.isFunction || request.type.isSuspendFunction) &&
            request.type.typeArguments.last().let {
                !it.isChildComponent && !it.isMergeChildComponent
            }) {
            val factoryExists = generateSequence(owner) { it.parent }
                .filter { it.componentFactoryType == request.type }
                .any()
            val assistedTypes = request.type.typeArguments.dropLast(1).distinct()
            if (!factoryExists && assistedTypes.isNotEmpty()) {
                val returnType = request.type.typeArguments.last()
                val childComponentType = moduleDescriptor.builtIns.any.toClassifierRef().defaultType
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
                    isExternal = false,
                    isInline = true,
                    visibility = DescriptorVisibilities.INTERNAL,
                    modality = Modality.FINAL,
                    isFunBinding = false
                )
                val childComponent = componentImplFactory(
                    childComponentType,
                    request.type,
                    owner.contextTreeNameProvider("${owner.rootComponent.name}_AC").asNameId(),
                    assistedTypes,
                    listOf(bindingCallable),
                    owner
                )
                this += AssistedBindingNode(
                    type = request.type,
                    owner = owner,
                    childComponent = childComponent,
                    assistedTypes = assistedTypes
                )
            }
        }

        if (request.type.isSubTypeOf(mapType)) {
            val mapEntries = buildList<Callable> {
                this += declarationStore.mapEntriesByType(request.type)
                    .filter { it.targetComponent.checkComponent() }
                parentsTopDown.forEach { parent ->
                    parent.implicitMapEntries[request.type]
                        ?.filter {
                            with(parent) {
                                it.targetComponent.checkComponent()
                            }
                        }
                        ?.let { this += it }
                }
                implicitMapEntries[request.type]
                    ?.filter { it.targetComponent.checkComponent() }
                    ?.let { this += it }
                parentsTopDown.forEach { parent ->
                    parent.explicitMapEntries[request.type]?.let { this += it }
                }
                explicitMapEntries[request.type]?.let { this += it }
            }
                .map { entry ->
                    entry.substitute(
                        getSubstitutionMap(listOf(request.type to entry.type))
                    )
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
            val setElements = buildList<Callable> {
                this += declarationStore.setElementsByType(request.type)
                    .filter { it.targetComponent.checkComponent() }
                parentsTopDown.forEach { parent ->
                    parent.implicitSetElements[request.type]
                        ?.filter {
                            with(parent) {
                                it.targetComponent.checkComponent()
                            }
                        }
                        ?.let { this += it }
                }
                implicitSetElements[request.type]
                    ?.filter { it.targetComponent.checkComponent() }
                    ?.let { this += it }
                parentsTopDown.forEach { parent ->
                    parent.explicitSetElements[request.type]?.let { this += it }
                }
                explicitSetElements[request.type]?.let { this += it }
            }
                .map { element ->
                    element.substitute(
                        getSubstitutionMap(listOf(request.type to element.type))
                    )
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

    private fun TypeRef?.checkComponent(): Boolean =
        this == null || owner.nonAssistedComponent.componentType.isAssignable(this)

    private fun Callable.getDependencies(type: TypeRef, isInterceptor: Boolean): List<BindingRequest> {
        val substitutionMap = getSubstitutionMap(listOf(type to this.type))
        return valueParameters
            .filter { !it.isFunApi }
            .map { it.toBindingRequest(this, substitutionMap) }
            .filter { !isInterceptor || it.type != this.type.substitute(substitutionMap) }
    }

    private fun getInterceptorsForType(
        type: TypeRef,
        callableKind: Callable.CallableKind
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
                val substitutionMap = getSubstitutionMap(listOf(providerType to interceptor.callable.type))
                val finalCallable = interceptor.callable.substitute(substitutionMap)
                interceptor.copy(
                    callable = finalCallable,
                    dependencies = finalCallable.getDependencies(type, true)
                )
            }
    }

    private fun getInterceptorsForType(providerType: TypeRef): List<InterceptorNode> = buildList<InterceptorNode> {
        this += explicitInterceptors
            .filter { providerType.isAssignable(it.callable.type) }
            .filter { it.callable.targetComponent.checkComponent() }
        this += parentsBottomUp.flatMap { parent ->
            parent.explicitInterceptors
                .filter { providerType.isAssignable(it.callable.type) }
                .filter {
                    with(parent) {
                        it.callable.targetComponent.checkComponent()
                    }
                }
        }
        this += implicitInterceptors
            .filter { providerType.isAssignable(it.callable.type) }
            .filter { it.callable.targetComponent.checkComponent() }
        this += parentsBottomUp.flatMap { parent ->
            parent.implicitInterceptors
                .filter { providerType.isAssignable(it.callable.type) }
                .filter {
                    with(parent) {
                        it.callable.targetComponent.checkComponent()
                    }
                }
        }
        this += declarationStore.interceptorsByType(providerType)
            .filter { it.targetComponent.checkComponent() }
            .map { interceptor ->
                InterceptorNode(
                    interceptor,
                    interceptor.getDependencies(interceptor.type, true)
                )
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

    private fun FunBindingDescriptor.toFunBindingNode(request: BindingRequest): FunBindingNode {
        val substitutionMap = getSubstitutionMap(listOf(request.type to type))
        val finalCallable = callable.substitute(substitutionMap)
        return FunBindingNode(
            type = request.type.substituteStars(type),
            rawType = originalType,
            owner = owner,
            dependencies = finalCallable.getDependencies(request.type, false),
            callable = finalCallable
        )
    }

    fun ValueParameterRef.toBindingRequest(
        callable: Callable,
        substitutionMap: Map<ClassifierRef, TypeRef>
    ): BindingRequest = BindingRequest(
        type = type.substitute(substitutionMap)
            .replaceTypeParametersWithStars(),
        origin = callable.fqName.child(name),
        required = !hasDefault,
        callableKind = callable.callableKind,
        lazy = callable.isFunBinding,
        forObjectCall = parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER &&
                type.classifier.isObject
    )

    private fun TypeRef.makeNonNullIfPossible(callable: Callable): TypeRef {
        if (!isMarkedNullable) return this
        if (callable.type.isMarkedNullable) return this
        return copy(isMarkedNullable = false)
    }

}

private fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"

@Qualifier
@Target(AnnotationTarget.TYPE)
annotation class Parent
