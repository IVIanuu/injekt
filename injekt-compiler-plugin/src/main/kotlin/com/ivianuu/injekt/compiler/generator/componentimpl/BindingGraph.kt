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
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.ModuleDescriptor
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.TypeTranslator
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.callableKind
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.isAssignable
import com.ivianuu.injekt.compiler.generator.isSubTypeOf
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.substituteStars
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
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
    private val moduleDescriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor,
    private val typeTranslator: TypeTranslator
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

    private val explicitBindings = mutableListOf<CallableWithReceiver>()
    private val implicitBindings = mutableListOf<CallableWithReceiver>()
    private val explicitInterceptors = mutableListOf<InterceptorNode>()
    private val implicitInterceptors = mutableListOf<InterceptorNode>()
    private val explicitMapEntries = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()
    private val implicitMapEntries = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()
    private val explicitSetElements = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()
    private val implicitSetElements = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()

    val resolvedBindings = mutableMapOf<TypeRef, BindingNode>()

    private val chain = mutableListOf<BindingRequest>()
    private var locked = false

    private val mapType = declarationStore.typeTranslator.toClassifierRef(
        moduleDescriptor.builtIns.map
    ).defaultType
    private val setType = declarationStore.typeTranslator.toClassifierRef(
        moduleDescriptor.builtIns.set
    ).defaultType

    init {
        declarationStore.moduleForType(owner.componentType)
            .collectContributions(
                parentAccessExpression = null,
                addBinding = { explicitBindings += it },
                addInterceptor = { explicitInterceptors += it },
                addMapEntries = {
                    explicitMapEntries.getOrPut(it.callable.type) { mutableListOf() } += it
                },
                addSetElements = {
                    explicitSetElements.getOrPut(it.callable.type) { mutableListOf() } += it
                }
            )

        declarationStore.allModules
            .filter { it.targetComponent.checkComponent(null) }
            .map { declarationStore.moduleForType(it.type) }
            .forEach { implicitModule ->
                if (implicitModule.type.classifier.isObject) {
                    implicitModule.collectContributions(
                        parentAccessExpression = {
                            emit("${implicitModule.type.classifier.fqName}")
                        },
                        addBinding = { implicitBindings += it },
                        addInterceptor = { implicitInterceptors += it },
                        addMapEntries = {
                            implicitMapEntries.getOrPut(it.callable.type) { mutableListOf() } += it
                        },
                        addSetElements = {
                            implicitSetElements.getOrPut(it.callable.type) { mutableListOf() } += it
                        }
                    )
                } else {
                    val callable = ComponentCallable(
                        name = implicitModule.type.uniqueTypeName(),
                        isOverride = false,
                        type = implicitModule.type,
                        body = null,
                        isProperty = true,
                        callableKind = Callable.CallableKind.DEFAULT,
                        initializer = {
                            emit("${implicitModule.type.classifier.fqName}")
                            if (!implicitModule.type.classifier.isObject)
                                emit("()")
                        },
                        isMutable = false,
                        isInline = false,
                        canBePrivate = true,
                        valueParameters = emptyList(),
                        typeParameters = emptyList()
                    ).also { owner.members += it }

                    implicitModule.collectContributions(
                        parentAccessExpression = { emit("${callable.name}") },
                        addBinding = { implicitBindings += it },
                        addInterceptor = { implicitInterceptors += it },
                        addMapEntries = {
                            implicitMapEntries.getOrPut(it.callable.type) { mutableListOf() } += it
                        },
                        addSetElements = {
                            implicitSetElements.getOrPut(it.callable.type) { mutableListOf() } += it
                        }
                    )
                }
            }
    }

    private fun ModuleDescriptor.collectContributions(
        parentAccessExpression: ComponentExpression?,
        addBinding: (CallableWithReceiver) -> Unit,
        addInterceptor: (InterceptorNode) -> Unit,
        addMapEntries: (CallableWithReceiver) -> Unit,
        addSetElements: (CallableWithReceiver) -> Unit
    ) {
        for (callable in callables) {
            if (callable.contributionKind == null) continue
            when (callable.contributionKind) {
                Callable.ContributionKind.BINDING -> addBinding(
                    CallableWithReceiver(
                        callable,
                        parentAccessExpression,
                        owner
                    )
                )
                Callable.ContributionKind.INTERCEPTOR -> addInterceptor(
                    InterceptorNode(
                        callable,
                        parentAccessExpression,
                        owner,
                        callable.getDependencies(callable.type, true)
                    )
                )
                Callable.ContributionKind.MAP_ENTRIES -> addMapEntries(
                    CallableWithReceiver(
                        callable,
                        parentAccessExpression,
                        owner
                    )
                )
                Callable.ContributionKind.SET_ELEMENTS -> addSetElements(
                    CallableWithReceiver(
                        callable,
                        parentAccessExpression,
                        owner
                    )
                )
                Callable.ContributionKind.MODULE -> declarationStore.moduleForType(callable.type)
                    .collectContributions(
                        parentAccessExpression.child(callable),
                        addBinding, addInterceptor, addMapEntries, addSetElements
                    )
            }.let {}
        }
    }

    private fun ComponentExpression?.child(callable: Callable): ComponentExpression = {
        if (this@child != null) {
            this@child()
            emit(".")
        }
        emit("${callable.name}")
        if (callable.isCall) emit("()")
    }

    fun checkRequests(requests: List<BindingRequest>) {
        requests.forEach { check(it) }
        requests.forEach { request ->
            val binding = getBinding(request)
            if (binding.callableKind != Callable.CallableKind.DEFAULT &&
                binding.callableKind != request.callableKind) {
                error("Call context mismatch. '${request.origin.orUnknown()}' is a ${request.callableKind.name} callable but " +
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
                error("Dependencies call context mismatch. Dependencies of '${binding.origin.orUnknown()}' have different call contexts\n" +
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
                    error("Call context mismatch. '${request.origin.orUnknown()}' is a ${request.callableKind.name} callable but " +
                            "dependency '${dependency.origin.orUnknown()}' is a ${dependency.callableKind.name} callable.")
                } else {
                    binding.callableKind = dependency.callableKind
                }
            }

        binding.refineType(binding.dependencies.map { getBinding(it) })

        // todo callable interceptors
        binding.interceptors = (if (binding is CallableBindingNode)
            binding.callable.getCallableInterceptors(binding.type) else emptyList()) +
                getInterceptorsForType(binding.type, binding.callableKind)

        binding.interceptors
            .forEach { interceptor ->
                chain.push(
                    BindingRequest(
                        interceptor.callable.type,
                        interceptor.callable.fqName,
                        true,
                        interceptor.callable.callableKind,
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
            .filterNot { it.lazy }
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
                relevantSubchain.any { it.lazy || !request.required || request.type.isMarkedNullable }) return
            error(
                "Circular dependency\n${relevantSubchain.joinToString("\n")} " +
                        "already contains\n$request\n\nDebug:\n${chain.joinToString("\n")}"
            )
        }
        chain.push(request)
        val binding = getBinding(request)
        if (request.type == owner.assistedRequests.singleOrNull()?.type &&
                binding is CallableBindingNode &&
                binding.eager) {
            error("Cannot perform assisted injection on a eager binding $request ${binding.callable.fqName}")
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

        error(
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

            error(
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

        val effectBindings = getEffectBindingsForType(request)
        // todo there should be only one valid effect binding
        binding = effectBindings.firstOrNull()
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
            .filter { it.callable.default == default }
            .filter { request.type.isAssignable(it.callable.type) }
            .map { (callable, receiver, bindingOwner) ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type)
                        .makeNonNullIfPossible(finalCallable),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = bindingOwner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = receiver,
                    callable = finalCallable
                )
            }
    }

    private fun getExplicitParentBindingsForType(parent: BindingGraph, request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += parent.explicitBindings
            .filter { it.callable.targetComponent.checkComponent(request.type) }
            .filter { request.type.isAssignable(it.callable.type) }
            .map { (callable, receiver, bindingOwner) ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type)
                        .makeNonNullIfPossible(finalCallable),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = bindingOwner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = receiver,
                    callable = finalCallable
                )
            }
    }

    private fun getImplicitUserBindingsForType(request: BindingRequest, default: Boolean): List<BindingNode> = buildList<BindingNode> {
        this += declarationStore.bindingsForType(request.type)
            .filter { it.default == default }
            .filter { it.targetComponent.checkComponent(request.type) }
            .map { callable ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type)
                        .makeNonNullIfPossible(finalCallable),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = null,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = null,
                    callable = finalCallable
                )
            }
        this += implicitBindings
            .filter { it.callable.default == default }
            .filter { request.type.isAssignable(it.callable.type) }
            .map { (callable, receiver, bindingOwner) ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type)
                        .makeNonNullIfPossible(finalCallable),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = bindingOwner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = receiver,
                    callable = finalCallable
                )
            }
    }

    private fun getImplicitFrameworkBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        if (request.type == owner.componentType) {
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
                        lazy = true
                    )
                ),
                origin = request.origin
            )
        }

        this += declarationStore.funBindingsForType(request.type)
            .filter { it.callable.targetComponent.checkComponent(request.type) }
            .map { funBinding ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to funBinding.type))
                val finalCallable = funBinding.callable.substitute(substitutionMap)
                FunBindingNode(
                    type = request.type.substituteStars(funBinding.type),
                    rawType = funBinding.originalType,
                    owner = owner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    callable = finalCallable
                )
            }

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
                val childComponentType = typeTranslator.toClassifierRef(
                    moduleDescriptor.builtIns.any
                ).defaultType
                val bindingCallable = Callable(
                    packageFqName = FqName.ROOT,
                    fqName = request.origin,
                    name = "invoke".asNameId(),
                    type = returnType,
                    effectType = returnType,
                    typeParameters = emptyList(),
                    valueParameters = emptyList(),
                    targetComponent = null,
                    scoped = false,
                    eager = false,
                    default = false,
                    contributionKind = null,
                    isCall = true,
                    callableKind = request.type.callableKind,
                    interceptors = emptyList(),
                    effects = emptyList(),
                    isExternal = false,
                    isInline = true,
                    visibility = DescriptorVisibilities.INTERNAL,
                    modality = Modality.FINAL,
                    receiver = null,
                    valueArgs = emptyMap(),
                    typeArgs = emptyMap(),
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
            val mapEntries = buildList<CallableWithReceiver> {
                this += declarationStore.mapEntriesByType(request.type)
                    .filter {
                        with(owner.graph) {
                            it.targetComponent.checkComponent(request.type)
                        }
                    }
                    .map { CallableWithReceiver(it, null, null) }
                implicitMapEntries[request.type]
                    ?.filter { it.callable.targetComponent.checkComponent(request.type) }
                    ?.let { this += it }
                parentsTopDown.forEach { parent ->
                    parent.explicitMapEntries[request.type]?.let { this += it }
                }
                explicitMapEntries[request.type]?.let { this += it }
            }
                .map { entry ->
                    entry.copy(
                        callable = entry.callable.substitute(
                            getSubstitutionMap(listOf(request.type to entry.callable.type))
                        )
                    )
                }
            if (mapEntries.isNotEmpty()) {
                val dependenciesByEntry = mapEntries.map { (entry) ->
                    entry to entry.valueParameters
                        .filter { it.argName == null }
                        .map {
                            BindingRequest(
                                it.type,
                                entry.fqName.child(it.name),
                                !it.hasDefault,
                                entry.callableKind,
                                entry.isFunBinding
                            )
                        }
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
            val setElements = buildList<CallableWithReceiver> {
                this += declarationStore.setElementsByType(request.type)
                    .filter {
                        with(owner.graph) {
                            it.targetComponent.checkComponent(request.type)
                        }
                    }
                    .map { CallableWithReceiver(it, null, null) }
                implicitSetElements[request.type]
                    ?.filter { it.callable.targetComponent.checkComponent(request.type) }
                    ?.let { this += it }
                parentsTopDown.forEach { parent ->
                    parent.explicitSetElements[request.type]?.let { this += it }
                }
                explicitSetElements[request.type]?.let { this += it }
            }
                .map { element ->
                    element.copy(
                        callable = element.callable.substitute(
                            getSubstitutionMap(listOf(request.type to element.callable.type))
                        )
                    )
                }
            if (setElements.isNotEmpty()) {
                val dependenciesByElement = setElements.map { (element) ->
                    element to element.valueParameters
                        .filter { it.argName == null }
                        .map {
                            BindingRequest(
                                it.type,
                                element.fqName.child(it.name),
                                !it.hasDefault,
                                element.callableKind,
                                element.isFunBinding
                            )
                        }
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

    private fun getEffectBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += declarationStore.effectBindingsFor(request.type)
            .filter { it.targetComponent.checkComponent(request.type) }
            .map { callable ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type)
                        .makeNonNullIfPossible(finalCallable),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = null,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = null,
                    callable = finalCallable
                )
            }
        this += declarationStore.effectFunBindingsFor(request.type)
            .filter { it.callable.targetComponent.checkComponent(request.type) }
            .map { funBinding ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to funBinding.type))
                val finalCallable = funBinding.callable.substitute(substitutionMap)
                FunBindingNode(
                    type = request.type.substituteStars(funBinding.type)
                        .makeNonNullIfPossible(finalCallable),
                    rawType = funBinding.originalType,
                    owner = owner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    callable = finalCallable
                )
            }
    }

    fun TypeRef?.checkComponent(type: TypeRef?): Boolean {
        return this == null || this == owner.componentType ||
                (owner.isAssisted && type == owner.assistedRequests.single().type)
    }

    private fun Callable.getCallableInterceptors(type: TypeRef): List<InterceptorNode> {
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
        return interceptors.map { interceptor ->
            val substitutionMap = getSubstitutionMap(listOf(providerType to interceptor.type))
            val finalInterceptor = interceptor.substitute(substitutionMap)
            InterceptorNode(
                finalInterceptor,
                null,
                null,
                finalInterceptor.getDependencies(type, true)
            )
        }.filter { providerType.isAssignable(it.callable.type) }
    }

    private fun Callable.getDependencies(type: TypeRef, isInterceptor: Boolean): List<BindingRequest> {
        val substitutionMap = getSubstitutionMap(listOf(type to this.type))
        return valueParameters
            .filter { it.argName == null && !it.isFunApi }
            .map { it.toBindingRequest(this, substitutionMap) }
            .filter { !isInterceptor || it.type != this.type.substitute(substitutionMap) }
    }

    private fun getInterceptorsForType(
        type: TypeRef,
        callableKind: Callable.CallableKind
    ): List<InterceptorNode> {
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
            .filter { it.callable.targetComponent.checkComponent(providerType.typeArguments.last()) }
        this += parentsBottomUp.flatMap { parent ->
            parent.explicitInterceptors
                .filter { providerType.isAssignable(it.callable.type) }
                .filter { it.callable.targetComponent.checkComponent(providerType.typeArguments.last()) }
        }
        this += declarationStore.interceptorsByType(providerType)
            .filter { it.targetComponent.checkComponent(providerType.typeArguments.last()) }
            .map { interceptor ->
                InterceptorNode(
                    interceptor,
                    null,
                    null,
                    interceptor.getDependencies(interceptor.type, true)
                )
            }
        this += implicitInterceptors
            .filter { providerType.isAssignable(it.callable.type) }
            .filter { it.callable.targetComponent.checkComponent(providerType.typeArguments.last()) }
    }

}

private fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"

@Qualifier
@Target(AnnotationTarget.TYPE)
annotation class Parent
