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
import com.ivianuu.injekt.compiler.generator.TypeTranslator
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.callableKind
import com.ivianuu.injekt.compiler.generator.copy
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.isAssignable
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.substituteStars
import com.ivianuu.injekt.compiler.generator.typeWith
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
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
        List<TypeRef>,
        List<Callable>,
        ComponentImpl?,
    ) -> ComponentImpl,
    private val moduleDescriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor,
    private val typeTranslator: TypeTranslator
) {

    private val parent = owner.parent?.graph

    private val moduleBindingCallables = mutableListOf<CallableWithReceiver>()
    private val parentModuleBindingCallables = owner.parent?.graph?.moduleBindingCallables
        ?: emptyList()
    private val moduleDecorators = mutableListOf<DecoratorNode>()
    private val parentModuleDecorators = owner.parent?.graph?.moduleDecorators
        ?: emptyList()

    private val collections: BindingCollections = collectionsFactory(owner, parent?.collections)

    val resolvedBindings = mutableMapOf<TypeRef, BindingNode>()

    private val chain = mutableListOf<BindingRequest>()
    private var locked = false

    init {
        fun ModuleDescriptor.collectContributions(parentAccessExpression: ComponentExpression?) {
            for (callable in callables) {
                if (callable.contributionKind == null) continue
                when (callable.contributionKind) {
                    Callable.ContributionKind.BINDING -> moduleBindingCallables += CallableWithReceiver(
                        callable,
                        parentAccessExpression,
                        owner
                    )
                    Callable.ContributionKind.DECORATOR -> moduleDecorators += DecoratorNode(
                        callable,
                        parentAccessExpression,
                        owner,
                        callable.getDependencies(callable.type, true)
                    )
                    Callable.ContributionKind.MAP_ENTRIES -> {
                        collections.addMapEntries(
                            CallableWithReceiver(
                                callable,
                                parentAccessExpression,
                                owner
                            )
                        )
                    }
                    Callable.ContributionKind.SET_ELEMENTS -> {
                        collections.addSetElements(
                            CallableWithReceiver(
                                callable,
                                parentAccessExpression,
                                owner
                            )
                        )
                    }
                    Callable.ContributionKind.MODULE -> {
                        declarationStore.moduleForType(callable.type)
                            .collectContributions(parentAccessExpression.child(callable))
                    }
                }.let {}
            }
        }

        declarationStore.moduleForType(owner.componentType)
            .collectContributions(null)
        owner.mergeDeclarations
            .filter { it.isModule }
            .map { declarationStore.moduleForType(it) }
            .onEach { includedModule ->
                if (includedModule.type.classifier.isObject) {
                    includedModule.collectContributions {
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
                        isMutable = false,
                        isInline = false,
                        canBePrivate = true,
                        valueParameters = emptyList(),
                        typeParameters = emptyList()
                    ).also { owner.members += it }

                    includedModule.collectContributions {
                        emit("${callable.name}")
                    }
                }
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

        // todo callable decorators
        binding.decorators = (if (binding is CallableBindingNode)
            binding.callable.getCallableDecorators(binding.type) else emptyList()) +
                getDecoratorsForType(binding.type, binding.callableKind)

        binding.decorators
            .forEach { decorator ->
                chain.push(
                    BindingRequest(
                        decorator.callable.type,
                        decorator.callable.fqName,
                        true,
                        decorator.callable.callableKind,
                        false
                    )
                )
                decorator.dependencies.forEach { dependency ->
                    check(dependency)
                }
                chain.pop()
            }
    }

    private fun check(request: BindingRequest) {
        if (request in chain) {
            val relevantSubchain = chain.subList(
                chain.indexOf(request), chain.size
            )
            if (request.lazy || relevantSubchain.any { it.lazy }) return
            error(
                "Circular dependency\n${relevantSubchain.joinToString("\n")} " +
                        "already contains\n$request\n\nDebug:\n${chain.joinToString("\n")}"
            )
        }
        chain.push(request)
        val binding = getBinding(request)
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

        val explicitBindings = getExplicitBindingsForType(request)
        binding = explicitBindings.mostSpecificOrFail("explicit")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        val (implicitInternalUserBindings, externalImplicitUserBindings) = getImplicitUserBindingsForType(request)
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

        val implicitFrameworkBindings = getImplicitFrameworkBindingsForType(request)
        binding = implicitFrameworkBindings.mostSpecificOrFail("implicit framework")
        binding?.let {
            resolvedBindings[request.type] = it
            return it
        }

        if (owner.isAssisted) {
            val explicitParentBindings = getExplicitParentBindingsForType(request)
            explicitParentBindings.singleOrNull()?.let {
                resolvedBindings[request.type] = it
                return it
            }
        }

        parent?.getBindingOrNull(request)?.let {
            resolvedBindings[request.type] = it
            return it
        }

        return null
    }

    private fun getExplicitBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += owner.additionalInputTypes
            .filter { request.type.isAssignable(it) }
            .map {
                InputBindingNode(
                    type = it,
                    owner = owner
                )
            }

        this += moduleBindingCallables
            .filter { request.type.isAssignable(it.callable.type) }
            .map { (callable, receiver, bindingOwner) ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = bindingOwner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = receiver,
                    callable = finalCallable
                )
            }
    }

    private fun getExplicitParentBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += parentModuleBindingCallables
            .filter { it.callable.targetComponent.checkComponent(request.type) }
            .filter { request.type.isAssignable(it.callable.type) }
            .map { (callable, receiver, bindingOwner) ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = bindingOwner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = receiver,
                    callable = finalCallable
                )
            }
    }

    private fun getImplicitUserBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += declarationStore.bindingsForType(request.type)
            .filter { it.targetComponent.checkComponent(request.type) }
            .map { callable ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type),
                    rawType = finalCallable.originalType,
                    owner = owner,
                    declaredInComponent = null,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    receiver = null,
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
            val assistedTypes = request.type.typeArguments.dropLast(1).distinct()
            if (assistedTypes.isNotEmpty()) {
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
                    contributionKind = null,
                    isCall = true,
                    callableKind = request.type.callableKind,
                    decorators = emptyList(),
                    effects = emptyList(),
                    isExternal = false,
                    isInline = true,
                    visibility = Visibilities.INTERNAL,
                    modality = Modality.FINAL,
                    receiver = null,
                    valueArgs = emptyMap(),
                    typeArgs = emptyMap(),
                    isFunBinding = false
                )
                val childComponent = componentImplFactory(
                    childComponentType,
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

        this += collections.getNodes(request)
    }

    private fun getEffectBindingsForType(request: BindingRequest): List<BindingNode> = buildList<BindingNode> {
        this += declarationStore.effectBindingsFor(request.type)
            .filter { it.targetComponent.checkComponent(request.type) }
            .map { callable ->
                val substitutionMap = getSubstitutionMap(listOf(request.type to callable.type))
                val finalCallable = callable.substitute(substitutionMap)
                CallableBindingNode(
                    type = request.type.substituteStars(finalCallable.type),
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
                    type = request.type.substituteStars(funBinding.type),
                    rawType = funBinding.originalType,
                    owner = owner,
                    dependencies = finalCallable.getDependencies(request.type, false),
                    callable = finalCallable
                )
            }
    }

    fun TypeRef?.checkComponent(type: TypeRef): Boolean {
        return this == null || this == owner.componentType ||
                (owner.isAssisted && type == owner.assistedRequests.single().type)
    }

    private fun Callable.getCallableDecorators(type: TypeRef): List<DecoratorNode> {
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
        return decorators.map { decorator ->
            val substitutionMap = getSubstitutionMap(listOf(providerType to decorator.type))
            val finalDecorator = decorator.substitute(substitutionMap)
            DecoratorNode(
                finalDecorator,
                null,
                null,
                finalDecorator.getDependencies(type, true)
            )
        }.filter { providerType.isAssignable(it.callable.type) }
    }

    private fun Callable.getDependencies(type: TypeRef, isDecorator: Boolean): List<BindingRequest> {
        val substitutionMap = getSubstitutionMap(listOf(type to this.type))
        return valueParameters
            .filter { it.argName == null && !it.isFunApi }
            .map { it.toBindingRequest(this, substitutionMap) }
            .filter { !isDecorator || it.type != this.type.substitute(substitutionMap) }
    }

    private fun getDecoratorsForType(
        type: TypeRef,
        callableKind: Callable.CallableKind
    ): List<DecoratorNode> {
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
        return getDecoratorsForType(providerType)
            .map { decorator ->
                val substitutionMap = getSubstitutionMap(listOf(providerType to decorator.callable.type))
                val finalCallable = decorator.callable.substitute(substitutionMap)
                decorator.copy(
                    callable = finalCallable,
                    dependencies = finalCallable.getDependencies(type, true)
                )
            }
    }

    private fun getDecoratorsForType(providerType: TypeRef): List<DecoratorNode> = buildList<DecoratorNode> {
        this += moduleDecorators
            .filter { providerType.isAssignable(it.callable.type) }
            .filter { it.callable.targetComponent.checkComponent(providerType.typeArguments.last()) }
        this += parentModuleDecorators
            .filter { providerType.isAssignable(it.callable.type) }
            .filter { it.callable.targetComponent.checkComponent(providerType.typeArguments.last()) }
        this += declarationStore.decoratorsByType(providerType)
            .filter { it.targetComponent.checkComponent(providerType.typeArguments.last()) }
            .map { decorator ->
                DecoratorNode(
                    decorator,
                    null,
                    null,
                    decorator.getDependencies(decorator.type, true)
                )
            }
    }
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
            ((parent?.getMapEntries(type) ?: emptyList()) +
                    (if (parent == null) declarationStore.mapEntriesByType(type)
                        .filter {
                            with(owner.graph) {
                                it.targetComponent.checkComponent(type)
                            }
                        }
                        .map { CallableWithReceiver(it, null, null) }
                    else emptyList()) +
                    (thisMapEntries[type] ?: emptyList()))
                .map { entry ->
                    entry.copy(
                        callable = entry.callable.substitute(
                            getSubstitutionMap(listOf(type to entry.callable.type))
                        )
                    )
                }
        }
    }

    private val setElementsByType = mutableMapOf<TypeRef, List<CallableWithReceiver>>()
    private fun getSetElements(type: TypeRef): List<CallableWithReceiver> {
        return setElementsByType.getOrPut(type) {
            ((parent?.getSetElements(type) ?: emptyList()) +
                    (if (parent == null) declarationStore.setElementsByType(type)
                        .filter {
                            with(owner.graph) {
                                it.targetComponent.checkComponent(type)
                            }
                        }
                        .map { CallableWithReceiver(it, null, null) }
                    else emptyList()) +
                    (thisSetElements[type] ?: emptyList()))
                .map { element ->
                    element.copy(
                        callable = element.callable.substitute(
                            getSubstitutionMap(listOf(type to element.callable.type))
                        )
                    )
                }
        }
    }

    fun getNodes(request: BindingRequest): List<BindingNode> {
        return listOfNotNull(
            getMapEntries(request.type)
                .takeIf { it.isNotEmpty() }
                ?.let { entries ->
                    val dependenciesByEntry = entries.map { (entry) ->
                        entry to entry.valueParameters
                            .filter { it.argName == null }
                            .map {
                                BindingRequest(
                                    it.type,
                                    entry.fqName.child(it.name),
                                    it.hasDefault,
                                    entry.callableKind,
                                    entry.isFunBinding
                                )
                            }
                    }.toMap()
                    MapBindingNode(
                        type = request.type,
                        owner = owner,
                        dependencies = dependenciesByEntry.flatMap { it.value },
                        entries = entries,
                        dependenciesByEntry = dependenciesByEntry
                    )
                },
            getSetElements(request.type)
                .takeIf { it.isNotEmpty() }
                ?.let { elements ->
                    val dependenciesByElement = elements.map { (element) ->
                        element to element.valueParameters
                            .filter { it.argName == null }
                            .map {
                                BindingRequest(
                                    it.type,
                                    element.fqName.child(it.name),
                                    it.hasDefault,
                                    element.callableKind,
                                    element.isFunBinding
                                )
                            }
                    }.toMap()
                    SetBindingNode(
                        type = request.type,
                        owner = owner,
                        dependencies = dependenciesByElement.flatMap { it.value },
                        elements = elements,
                        dependenciesByElement = dependenciesByElement
                    )
                }
        )
    }
}
