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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.findPropertyGetter
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.getFunctionFromLambdaExpression
import com.ivianuu.injekt.compiler.getInjectConstructor
import com.ivianuu.injekt.compiler.getQualifierFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

typealias BindingResolver = (Key) -> List<BindingNode>

class ChildFactoryBindingResolver(
    private val parentFactory: ImplFactory,
    descriptor: IrClass
) : BindingResolver {

    private val childFactories =
        mutableMapOf<Key, MutableList<Lazy<ChildFactoryBindingNode>>>()

    init {
        descriptor
            .functions
            .filter { it.hasAnnotation(InjektFqNames.AstChildFactory) }
            .forEach { function ->
                val key =
                    parentFactory.pluginContext.irBuiltIns.function(function.valueParameters.size)
                        .typeWith(function.valueParameters.map { it.type } + function.returnType)
                        .withNoArgAnnotations(
                            parentFactory.pluginContext,
                            listOf(InjektFqNames.ChildFactory)
                        )
                        .asKey()

                childFactories.getOrPut(key) { mutableListOf() } += childFactoryBindingNode(
                    key, function
                )
            }
    }

    override fun invoke(requestedKey: Key): List<BindingNode> =
        childFactories[requestedKey]?.map { it.value } ?: emptyList()

    private fun childFactoryBindingNode(
        key: Key,
        function: IrFunction
    ) = lazy {
        val superType = function.returnType

        val moduleClass =
            function.getClassFromSingleValueAnnotation(
                InjektFqNames.AstClassPath,
                parentFactory.pluginContext
            )

        val moduleFunction = parentFactory.declarationStore
            .getModuleFunctionForClass(moduleClass)

        val fqName = FqName(
            function.getAnnotation(InjektFqNames.AstName)!!
                .getValueArgument(0)
                .let { it as IrConst<String> }
                .value
        )

        val childFactoryExpression = InjektDeclarationIrBuilder(
            parentFactory.pluginContext,
            moduleClass.symbol
        ).irLambda(key.type) { lambda ->
            val moduleVariable = irTemporary(
                irCall(moduleFunction).apply {
                    lambda.valueParameters.forEach {
                        putValueArgument(it.index, irGet(it))
                    }
                    putValueArgument(moduleFunction.valueParameters.lastIndex, irNull())
                }
            )
            val childFactoryImpl = ImplFactory(
                factoryFunction = lambda,
                origin = fqName,
                parent = parentFactory,
                superType = superType,
                moduleVariable = moduleVariable,
                moduleClass = moduleClass,
                pluginContext = parentFactory.pluginContext,
                symbols = parentFactory.symbols,
                declarationStore = parentFactory.declarationStore
            )

            +irReturn(childFactoryImpl.getImplExpression())
        }

        return@lazy ChildFactoryBindingNode(
            key = key,
            owner = parentFactory,
            origin = moduleClass.fqNameForIrSerialization,
            parent = parentFactory.clazz,
            childFactoryExpression = { childFactoryExpression }
        )
    }
}

class DependencyBindingResolver(
    private val moduleNode: ModuleNode,
    private val dependencyNode: DependencyNode,
    private val factory: AbstractFactory
) : BindingResolver {

    private val allDependencyFunctions = dependencyNode.dependency
        .declarations
        .mapNotNull { declaration ->
            when (declaration) {
                is IrFunction -> declaration
                is IrProperty -> declaration.getter
                else -> null
            }
        }
        .filter {
            it.valueParameters.isEmpty()
                    && !it.isFakeOverride &&
                    it.dispatchReceiverParameter!!.type != factory.pluginContext.irBuiltIns.anyType
        }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return allDependencyFunctions
            .filter { it.returnType.asKey() == requestedKey }
            .map { dependencyFunction ->
                DependencyBindingNode(
                    key = requestedKey,
                    function = dependencyFunction,
                    requirementNode = dependencyNode,
                    owner = factory,
                    origin = moduleNode.module.fqNameForIrSerialization
                )
            }
    }
}

class ModuleBindingResolver(
    private val moduleNode: ModuleNode,
    descriptor: IrClass,
    private val factory: AbstractFactory
) : BindingResolver {

    // todo make this the actual module function name origin or even better the exact location
    private val moduleRequestOrigin = moduleNode.module.descriptor
        .fqNameSafe

    private val bindingFunctions = descriptor
        .declarations
        .filterIsInstance<IrFunction>()

    private val allBindings = bindingFunctions
        .filter { it.hasAnnotation(InjektFqNames.AstBinding) }
        .map { bindingFunction ->
            val bindingKey = bindingFunction.returnType
                .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                .asKey()
            val propertyName = bindingFunction.getAnnotation(InjektFqNames.AstPropertyPath)!!
                .getValueArgument(0).let { it as IrConst<String> }.value

            val propertyGetter = moduleNode.module
                .findPropertyGetter(propertyName)

            val scoped = bindingFunction.hasAnnotation(InjektFqNames.AstScoped)

            when {
                bindingFunction.hasAnnotation(InjektFqNames.AstInstance) -> {
                    InstanceBindingNode(
                        key = bindingKey,
                        requirementNode = InstanceNode(
                            key = propertyGetter.returnType
                                .substituteAndKeepQualifiers(moduleNode.typeParametersMap)
                                .asKey(),
                            accessor = {
                                irCall(propertyGetter).apply {
                                    dispatchReceiver = moduleNode.accessor(this@InstanceNode)
                                }
                            }
                        ),
                        owner = factory,
                        origin = moduleNode.module.fqNameForIrSerialization
                    )
                }
                else -> {
                    if (bindingFunction.valueParameters.any {
                            it.descriptor.hasAnnotation(
                                InjektFqNames.AstAssisted
                            )
                        }) {
                        val assistedValueParameters = bindingFunction.valueParameters
                            .filter { it.descriptor.hasAnnotation(InjektFqNames.AstAssisted) }

                        val assistedFactoryType =
                            factory.pluginContext.irBuiltIns.function(assistedValueParameters.size)
                                .typeWith(
                                    assistedValueParameters
                                        .map {
                                            it.type
                                                .substituteAndKeepQualifiers(
                                                    moduleNode.descriptorTypeParametersMap
                                                )
                                        } + bindingKey.type
                                        .substituteAndKeepQualifiers(
                                            moduleNode.descriptorTypeParametersMap
                                        )
                                ).withNoArgAnnotations(
                                    factory.pluginContext,
                                    listOf(InjektFqNames.Provider)
                                )

                        val parameters = bindingFunction.valueParameters
                            .map {
                                InjektDeclarationIrBuilder.FactoryParameter(
                                    it.name.asString(),
                                    it.type
                                        .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap),
                                    it.hasAnnotation(InjektFqNames.AstAssisted)
                                )
                            }

                        val dependencies = parameters
                            .filterNot { it.assisted }
                            .map { BindingRequest(it.type.asKey(), moduleRequestOrigin) }

                        AssistedProvisionBindingNode(
                            key = assistedFactoryType.asKey(),
                            dependencies = dependencies,
                            targetScope = null,
                            scoped = scoped,
                            module = moduleNode,
                            provisionFunctionExpression = {
                                irCall(propertyGetter).apply {
                                    dispatchReceiver =
                                        moduleNode.accessor(this@AssistedProvisionBindingNode)
                                }
                            },
                            parameters = parameters,
                            owner = factory,
                            origin = moduleNode.module.fqNameForIrSerialization
                        )
                    } else {
                        val dependencies = bindingFunction.valueParameters
                            .map {
                                it.type
                                    .substituteAndKeepQualifiers(
                                        moduleNode.descriptorTypeParametersMap
                                    )
                                    .asKey()
                            }
                            .map { BindingRequest(it, moduleRequestOrigin) }

                        ProvisionBindingNode(
                            key = bindingKey,
                            dependencies = dependencies,
                            targetScope = null,
                            scoped = scoped,
                            module = moduleNode,
                            provisionFunctionExpression = {
                                irCall(propertyGetter).apply {
                                    dispatchReceiver =
                                        moduleNode.accessor(this@ProvisionBindingNode)
                                }
                            },
                            owner = factory,
                            origin = moduleNode.module.fqNameForIrSerialization
                        )
                    }
                }
            }
        }

    private val delegateBindings = bindingFunctions
        .filter { it.hasAnnotation(InjektFqNames.AstAlias) }
        .map { delegateFunction ->
            DelegateBindingNode(
                key = delegateFunction.returnType
                    .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                    .asKey(),
                originalKey = delegateFunction.valueParameters.single().type
                    .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                    .asKey(),
                owner = factory,
                requestOrigin = moduleRequestOrigin,
                origin = moduleNode.module.fqNameForIrSerialization
            )
        }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return (allBindings + delegateBindings)
            .filter { it.key == requestedKey }
    }
}

class MembersInjectorBindingResolver(
    private val symbols: InjektSymbols,
    private val declarationStore: InjektDeclarationStore,
    private val factoryImpl: AbstractFactory
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (InjektFqNames.MembersInjector !in requestedKey.type.getQualifierFqNames()) return emptyList()
        if (requestedKey.type.classOrNull != factoryImpl.pluginContext.irBuiltIns.function(1)) return emptyList()
        val target =
            requestedKey.type.typeArguments.first().typeOrNull?.getClass() ?: return emptyList()
        val membersInjector = declarationStore.getMembersInjectorForClassOrNull(target)
            ?: symbols.noOpMembersInjector.owner
        return listOf(
            MembersInjectorBindingNode(
                key = requestedKey,
                membersInjector = membersInjector,
                owner = factoryImpl,
                origin = target.fqNameForIrSerialization
            )
        )
    }
}

class AnnotatedClassBindingResolver(
    private val pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore,
    private val factory: AbstractFactory
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        return if (requestedKey.type.isFunction() &&
            requestedKey.type.classOrNull != pluginContext.irBuiltIns.function(0) &&
            InjektFqNames.Provider in requestedKey.type.getQualifierFqNames()
        ) {
            val clazz =
                requestedKey.type.typeArguments.last().typeOrNull?.getClass() ?: return emptyList()

            val constructor = clazz.getInjectConstructor()

            val scopeAnnotation = clazz.descriptor.getAnnotatedAnnotations(
                InjektFqNames.Scope,
                clazz.descriptor.module
            ).singleOrNull()
                ?: constructor?.descriptor?.getAnnotatedAnnotations(
                    InjektFqNames.Scope,
                    clazz.descriptor.module
                )
                    ?.singleOrNull()

            if (scopeAnnotation == null &&
                !clazz.hasAnnotation(InjektFqNames.Transient) &&
                constructor?.hasAnnotation(InjektFqNames.Transient) != true
            ) return emptyList()

            val scoped = scopeAnnotation != null

            val typeParametersMap = clazz
                .typeParameters
                .map { it.symbol }
                .associateWith { requestedKey.type.typeArguments[it.owner.index].typeOrFail }

            val factoryLambda = InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .classFactoryLambda(
                    clazz, declarationStore.getMembersInjectorForClassOrNull(clazz)
                )

            val factoryLambdaExpression = factory.factoryMembers.cachedValue(
                factoryLambda.type.asKey()
            ) { factoryLambda }

            val parameters = factoryLambda.getFunctionFromLambdaExpression()
                .valueParameters
                .map { valueParameter ->
                    InjektDeclarationIrBuilder.FactoryParameter(
                        valueParameter.name.asString(),
                        valueParameter.type
                            .substituteAndKeepQualifiers(typeParametersMap),
                        valueParameter.type.hasAnnotation(InjektFqNames.AstAssisted)
                    )
                }

            val dependencies = factoryLambda.getFunctionFromLambdaExpression()
                .valueParameters
                .filterNot { it.type.hasAnnotation(InjektFqNames.AstAssisted) }
                .map { valueParameter ->
                    BindingRequest(
                        valueParameter.type
                            .substituteAndKeepQualifiers(typeParametersMap)
                            .asKey(),
                        constructor?.valueParameters
                            ?.singleOrNull { it.name == valueParameter.name }
                            ?.descriptor
                            ?.fqNameSafe ?: clazz.properties
                            .singleOrNull { it.name == valueParameter.name }
                            ?.descriptor?.fqNameSafe
                    )
                }

            listOf(
                AssistedProvisionBindingNode(
                    key = requestedKey,
                    dependencies = dependencies,
                    targetScope = scopeAnnotation?.fqName,
                    scoped = scoped,
                    module = null,
                    provisionFunctionExpression = factoryLambdaExpression,
                    parameters = parameters,
                    owner = factory,
                    origin = clazz.fqNameForIrSerialization
                )
            )
        } else {
            val clazz = requestedKey.type.classOrNull?.owner ?: return emptyList()
            val constructor = clazz.getInjectConstructor()

            val scopeAnnotation = clazz.descriptor.getAnnotatedAnnotations(
                    InjektFqNames.Scope,
                    clazz.descriptor.module
                )
                .singleOrNull()
                ?: constructor?.descriptor?.getAnnotatedAnnotations(
                    InjektFqNames.Scope,
                    clazz.descriptor.module
                )
                    ?.singleOrNull()

            if (scopeAnnotation == null &&
                !clazz.hasAnnotation(InjektFqNames.Transient) &&
                constructor?.hasAnnotation(InjektFqNames.Transient) != true
            ) return emptyList()

            val scoped = scopeAnnotation != null

            val typeParametersMap = clazz
                .typeParameters
                .map { it.symbol }
                .associateWith { requestedKey.type.typeArguments[it.owner.index].typeOrFail }

            val factoryLambda = InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .classFactoryLambda(
                    clazz, declarationStore
                        .getMembersInjectorForClassOrNull(clazz)
                )
            val factoryLambdaExpression = factory.factoryMembers.cachedValue(
                factoryLambda.type.asKey()
            ) { factoryLambda }

            val dependencies = factoryLambda.getFunctionFromLambdaExpression()
                .valueParameters
                .filterNot { it.type.hasAnnotation(InjektFqNames.AstAssisted) }
                .map { valueParameter ->
                    BindingRequest(
                        valueParameter.type
                            .substituteAndKeepQualifiers(typeParametersMap)
                            .asKey(),
                        constructor?.valueParameters
                            ?.singleOrNull { it.name == valueParameter.name }
                            ?.descriptor
                            ?.fqNameSafe ?: clazz.properties
                            .singleOrNull { it.name == valueParameter.name }
                            ?.descriptor?.fqNameSafe
                    )
                }

            listOf(
                ProvisionBindingNode(
                    key = requestedKey,
                    dependencies = dependencies,
                    targetScope = scopeAnnotation?.fqName,
                    scoped = scoped,
                    module = null,
                    provisionFunctionExpression = factoryLambdaExpression,
                    owner = factory,
                    origin = clazz.fqNameForIrSerialization
                )
            )
        }
    }
}

class MapBindingResolver(
    private val pluginContext: IrPluginContext,
    private val factory: AbstractFactory,
    private val parent: MapBindingResolver?
) : BindingResolver {

    private val mapBuilders = mutableMapOf<Key, MultiBindingMap>()
    private val finalMaps: Map<Key, MultiBindingMap> by lazy {
        val mergedMaps: MutableMap<Key, MultiBindingMap> = parent?.finalMaps
            ?.mapValues { MultiBindingMap(it.value.origin, it.value.entries.toMutableMap()) }
            ?.toMutableMap() ?: mutableMapOf()
        mapBuilders.forEach { (mapKey, map) ->
            val mergedMap = mergedMaps.getOrPut(mapKey) {
                MultiBindingMap(map.origin, mutableMapOf())
            }
            map.entries.forEach { (entryKey, entryValue) ->
                val existing = mergedMap.entries[entryKey]
                if (existing != null) {
                    error(
                        "Cannot bind '${entryValue.key}' with key '$entryKey' declared at '${entryValue.requestOrigin.orUnknown()}' " +
                                "into map '$mapKey' declared at '${map.origin}'. Value was already bound at '${existing.requestOrigin.orUnknown()}'"
                    )
                }

                mergedMap.entries[entryKey] = entryValue
            }
        }
        mergedMaps
    }

    private data class MultiBindingMap(
        val origin: FqName,
        val entries: MutableMap<MapKey, BindingRequest>
    )

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return finalMaps
            .flatMap { (mapKey, map) ->
                listOf(
                    MapBindingNode(
                        mapKey,
                        factory,
                        map.origin,
                        map.entries
                    ),
                    frameworkBinding(InjektFqNames.Lazy, mapKey, map),
                    frameworkBinding(InjektFqNames.Provider, mapKey, map)
                )
            }
            .filter { it.key == requestedKey }
    }

    fun addMap(mapKey: Key, origin: FqName) {
        mapBuilders.getOrPut(mapKey) { MultiBindingMap(origin, mutableMapOf()) }
    }

    fun putMapEntry(
        mapKey: Key,
        entryKey: MapKey,
        entryValue: BindingRequest
    ) {
        val map = mapBuilders[mapKey]!!
        val existing = map.entries[entryKey]
        if (existing != null) {
            error(
                "Cannot bind '${entryValue.key}' with key '$entryKey' declared at '${entryValue.requestOrigin.orUnknown()}' " +
                        "into map '$mapKey' declared at '${map.origin}'. Value was already bound at '${existing.requestOrigin.orUnknown()}'"
            )
        }

        map.entries[entryKey] = entryValue
    }

    private fun frameworkBinding(
        qualifier: FqName,
        mapKey: Key,
        map: MultiBindingMap
    ) = MapBindingNode(
        pluginContext.symbolTable.referenceClass(pluginContext.builtIns.map)
            .typeWith(
                mapKey.type.typeArguments[0].typeOrFail,
                pluginContext.irBuiltIns.function(0)
                    .typeWith(mapKey.type.typeArguments[1].typeOrFail)
                    .withNoArgAnnotations(pluginContext, listOf(qualifier))
            )
            .asKey(),
        factory,
        map.origin,
        map.entries
    )
}


class SetBindingResolver(
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val factoryImplementation: AbstractFactory,
    private val parent: SetBindingResolver?
) : BindingResolver {

    private val setBuilders = mutableMapOf<Key, MultiBindingSet>()
    private val finalSets: Map<Key, MultiBindingSet> by lazy {
        val mergedSets: MutableMap<Key, MultiBindingSet> = parent?.finalSets
            ?.mapValues { MultiBindingSet(it.value.origin, it.value.elements.toMutableSet()) }
            ?.toMutableMap() ?: mutableMapOf()
        setBuilders.forEach { (setKey, set) ->
            val mergedSet = mergedSets.getOrPut(setKey) {
                MultiBindingSet(set.origin, mutableSetOf())
            }
            set.elements.forEach { element ->
                val existing = mergedSet.elements.singleOrNull { it.key == element.key }
                if (existing != null) {
                    error(
                        "Cannot bind '${element.key}' declared at '${element.requestOrigin.orUnknown()}' " +
                                "into set '$setKey' declared at '${set.origin}'. It was already bound at '${existing.requestOrigin.orUnknown()}'"
                    )
                }

                mergedSet.elements += element
            }
        }
        mergedSets
    }

    private data class MultiBindingSet(
        val origin: FqName?,
        val elements: MutableSet<BindingRequest>
    )

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return finalSets
            .flatMap { (setKey, set) ->
                listOf(
                    SetBindingNode(
                        setKey,
                        factoryImplementation,
                        set.origin,
                        set.elements
                    ),
                    frameworkBinding(InjektFqNames.Lazy, setKey, set),
                    frameworkBinding(InjektFqNames.Provider, setKey, set)
                )
            }
            .filter { it.key == requestedKey }
    }

    fun addSet(setKey: Key, origin: FqName) {
        setBuilders.getOrPut(setKey) { MultiBindingSet(origin, mutableSetOf()) }
    }

    fun addSetElement(setKey: Key, element: BindingRequest) {
        val set = setBuilders[setKey]!!
        val existing = set.elements.singleOrNull { it.key == element.key }
        if (existing != null) {
            error(
                "Cannot bind '${element.key}' declared at '${element.requestOrigin.orUnknown()}' " +
                        "into set '$setKey' declared at '${set.origin}'. It was already bound at '${existing.requestOrigin.orUnknown()}'"
            )
        }

        set.elements += element
    }

    private fun frameworkBinding(
        qualifier: FqName,
        setKey: Key,
        set: MultiBindingSet
    ) = SetBindingNode(
        pluginContext.symbolTable.referenceClass(pluginContext.builtIns.set)
            .typeWith(
                pluginContext.irBuiltIns.function(0).typeWith(
                    setKey.type.typeArguments.single().typeOrFail
                ).withNoArgAnnotations(pluginContext, listOf(qualifier))
            ).asKey(),
        factoryImplementation,
        set.origin,
        set.elements
    )
}

class LazyOrProviderBindingResolver(
    private val symbols: InjektSymbols,
    private val factory: AbstractFactory
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val requestedType = requestedKey.type
        return when {
            requestedType.isFunction() &&
                    requestedKey.type.classOrNull == factory.pluginContext.irBuiltIns.function(0) &&
                    InjektFqNames.Lazy in requestedType.getQualifierFqNames() ->
                listOf(
                    LazyBindingNode(
                        requestedKey,
                        null,
                        factory
                    )
                )
            requestedType.isFunction() &&
                    requestedKey.type.classOrNull == factory.pluginContext.irBuiltIns.function(0) &&
                    InjektFqNames.Provider in requestedType.getQualifierFqNames() ->
                listOf(
                    ProviderBindingNode(
                        requestedKey,
                        factory,
                        null
                    )
                )
            else -> emptyList()
        }
    }
}

class FactoryImplementationBindingResolver(
    private val factoryImplementationNode: FactoryImplementationNode
) : BindingResolver {
    private val factorySuperClassKey =
        factoryImplementationNode.key.type.classOrNull!!.superTypes().single()
            .asKey()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (requestedKey != factorySuperClassKey &&
            requestedKey != factoryImplementationNode.key
        ) return emptyList()
        return listOf(
            FactoryImplementationBindingNode(
                factoryImplementationNode
            )
        )
    }
}
