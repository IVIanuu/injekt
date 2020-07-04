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
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.findPropertyGetter
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.getFunctionParameterTypes
import com.ivianuu.injekt.compiler.getFunctionReturnType
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.getInjectConstructor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isAssistedProvider
import com.ivianuu.injekt.compiler.isNoArgProvider
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import com.ivianuu.injekt.compiler.withAnnotations
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias BindingResolver = (Key) -> List<BindingNode>

class ChildFactoryBindingResolver(
    private val parentFactory: FactoryImpl,
    descriptor: IrClass
) : BindingResolver {

    private val childFactories =
        mutableMapOf<Key, MutableList<Lazy<ChildFactoryBindingNode>>>()

    init {
        descriptor
            .functions
            .filter { it.hasAnnotation(InjektFqNames.AstChildFactory) }
            .forEach { function ->
                val key = function.getFunctionType(parentFactory.pluginContext)
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
            val moduleAccessor = if (moduleClass.kind == ClassKind.OBJECT) {
                val expr: FactoryExpression = { irGetObject(moduleClass.symbol) }
                expr
            } else {
                val moduleFunction = parentFactory.declarationStore
                    .getModuleFunctionForClass(moduleClass)
                val moduleVariable = irTemporary(
                    irCall(moduleFunction).apply {
                        lambda.valueParameters.forEach {
                            putValueArgument(it.index, irGet(it))
                        }
                    }
                )
                val expr: FactoryExpression = { irGet(moduleVariable) }
                expr
            }
            val childFactoryImpl = FactoryImpl(
                factoryFunction = lambda,
                origin = fqName,
                parent = parentFactory,
                superType = superType,
                scope = function.getClassFromSingleValueAnnotationOrNull(
                    InjektFqNames.AstScope, parentFactory.pluginContext
                )
                    ?.defaultType
                    ?: function.returnType,
                factoryModuleAccessor = moduleAccessor,
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
    private val factory: FactoryImpl
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
    private val factory: FactoryImpl
) : BindingResolver {

    // todo make this the actual module function name origin or even better the exact location
    private val moduleRequestOrigin = moduleNode.module.descriptor
        .fqNameSafe

    private val bindingFunctions = descriptor
        .declarations
        .filterIsInstance<IrFunction>()

    private val allBindings = bindingFunctions
        .filter { it.hasAnnotation(InjektFqNames.AstBinding) }
        .filterNot { it.hasAnnotation(InjektFqNames.AstTypeParameterPath) }
        .map { bindingFunction ->
            val bindingKey = bindingFunction.returnType
                .substituteAndKeepQualifiers(moduleNode.descriptorTypeParametersMap)
                .asKey()
            val propertyName = bindingFunction.getAnnotation(InjektFqNames.AstPropertyPath)!!
                .getValueArgument(0).let { it as IrConst<String> }.value

            val propertyGetter = moduleNode.module
                .findPropertyGetter(propertyName)

            val scoped = bindingFunction.hasAnnotation(InjektFqNames.AstScoped)

            val providerType = propertyGetter.returnType
                .substituteAndKeepQualifiers(moduleNode.typeParametersMap)

            val readerContext = propertyGetter.returnType.getFunctionParameterTypes()
                .last().classOrNull!!.owner

            val dependencies = mutableListOf<BindingRequest>()

            dependencies += BindingRequest(factory.factoryNode.key, null, null)

            val processedSuperTypes = mutableSetOf<IrType>()

            fun collectDependencies(
                superClass: IrClass,
                typeArguments: List<IrType>,
                typeParametersMap: Map<IrTypeParameterSymbol, IrType>
            ) {
                if (superClass.defaultType in processedSuperTypes) return
                processedSuperTypes += superClass.defaultType
                for (declaration in superClass.declarations.toList()) {
                    if (declaration !is IrFunction) continue
                    if (declaration is IrConstructor) continue
                    if (declaration.isFakeOverride) continue
                    if (declaration.dispatchReceiverParameter?.type == factory.pluginContext.irBuiltIns.anyType) break
                    dependencies += BindingRequest(
                        key = declaration.returnType
                            .substituteAndKeepQualifiers(
                                superClass.typeParameters
                                    .map { it.symbol }
                                    .zip(typeArguments)
                                    .toMap()
                            )
                            .substituteAndKeepQualifiers(
                                superClass.typeParameters
                                    .map { it.symbol }
                                    .zip(typeParametersMap.values)
                                    .toMap()
                            )
                            .asKey(),
                        requestingKey = bindingKey,
                        requestOrigin = superClass.getAnnotation(InjektFqNames.AstName)
                            ?.getValueArgument(0)
                            ?.let { it as IrConst<String> }
                            ?.value
                            ?.let { FqName(it) },
                    )
                }

                superClass.superTypes
                    .map { it to it.classOrNull?.owner }
                    .forEach { (superType, clazz) ->
                        if (clazz != null)
                            collectDependencies(
                                clazz,
                                superType.typeArguments.map { it.typeOrFail },
                                emptyMap()
                            )
                    }
            }

            readerContext.superTypes.forEach { superType ->
                collectDependencies(
                    superType.getClass()!!,
                    superType.typeArguments.map { it.typeOrFail },
                    moduleNode.typeParametersMap
                )
            }

            val parameters = mutableListOf<InjektDeclarationIrBuilder.FactoryParameter>()

            parameters += providerType
                .getFunctionParameterTypes()
                .dropLast(1)
                .mapIndexed { index, type ->
                    InjektDeclarationIrBuilder.FactoryParameter(
                        name = "p$index",
                        type = type,
                        assisted = true
                    )
                }

            parameters += InjektDeclarationIrBuilder.FactoryParameter(
                name = "_context",
                type = readerContext.defaultType,
                assisted = false
            )

            val assistedParameters = parameters.filter { it.assisted }

            if (assistedParameters.isNotEmpty()) {
                val assistedValueParameters = providerType
                    .getFunctionParameterTypes()
                    .dropLast(1)

                val assistedFactoryType =
                    factory.pluginContext.tmpFunction(assistedValueParameters.size)
                        .typeWith(
                            assistedValueParameters + providerType
                                .getFunctionReturnType()
                        ).withNoArgAnnotations(
                            factory.pluginContext,
                            listOf(InjektFqNames.Provider)
                        )

                AssistedProvisionBindingNode(
                    key = assistedFactoryType.asKey(),
                    context = readerContext,
                    dependencies = dependencies,
                    targetScope = null,
                    scoped = scoped,
                    module = moduleNode,
                    createExpression = { parametersMap ->
                        irCall(propertyGetter.returnType
                            .classOrNull!!
                            .functions
                            .single { it.owner.name.asString() == "invoke" }
                        ).apply {
                            dispatchReceiver = irCall(propertyGetter).apply {
                                dispatchReceiver =
                                    moduleNode.accessor(this@AssistedProvisionBindingNode)
                            }

                            parametersMap.values.forEachIndexed { index, expression ->
                                putValueArgument(
                                    index,
                                    expression()
                                )
                            }
                        }
                    },
                    parameters = parameters,
                    owner = factory,
                    origin = moduleNode.module.fqNameForIrSerialization
                )
            } else {
                ProvisionBindingNode(
                    key = bindingKey,
                    context = readerContext,
                    dependencies = dependencies,
                    targetScope = null,
                    scoped = scoped,
                    module = moduleNode,
                    createExpression = { parametersMap ->
                        irCall(providerType
                            .classOrNull!!
                            .functions
                            .single { it.owner.name.asString() == "invoke" }
                        ).apply {
                            dispatchReceiver = irCall(propertyGetter).apply {
                                dispatchReceiver =
                                    moduleNode.accessor(this@ProvisionBindingNode)
                            }

                            parametersMap.values.forEachIndexed { index, expression ->
                                putValueArgument(
                                    index,
                                    expression()
                                )
                            }
                        }
                    },
                    parameters = parameters,
                    owner = factory,
                    origin = moduleNode.module.fqNameForIrSerialization
                )
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

class AnnotatedClassBindingResolver(
    private val pluginContext: IrPluginContext,
    private val factory: FactoryImpl
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        return if (requestedKey.type.isAssistedProvider()) {
            val clazz =
                requestedKey.type.typeArguments.last().typeOrNull?.getClass() ?: return emptyList()

            val constructor = clazz.getInjectConstructor()

            val scope = clazz.getClassFromSingleValueAnnotationOrNull(
                InjektFqNames.Scoped, pluginContext
            )

            if (scope == null &&
                !clazz.hasAnnotation(InjektFqNames.Unscoped) &&
                constructor?.hasAnnotation(InjektFqNames.Unscoped) != true
            ) return emptyList()

            val parametersNameProvider = NameProvider()

            val constructorParameters = constructor?.valueParameters?.map { valueParameter ->
                InjektDeclarationIrBuilder.FactoryParameter(
                    name = parametersNameProvider.allocateForGroup(valueParameter.name).asString(),
                    type = valueParameter.type,
                    assisted = valueParameter.descriptor.hasAnnotation(InjektFqNames.Assisted)
                )
            } ?: emptyList()

            val typeParametersMap = clazz
                .typeParameters
                .map { it.symbol }
                .associateWith { requestedKey.type.typeArguments[it.owner.index].typeOrFail }

            val dependencies = constructorParameters
                .filterNot { it.assisted }
                .map { parameter ->
                    BindingRequest(
                        key = parameter.type
                            .substituteAndKeepQualifiers(typeParametersMap)
                            .asKey(),
                        requestingKey = requestedKey,
                        requestOrigin = constructor?.valueParameters
                            ?.singleOrNull { it.name.asString() == parameter.name }
                            ?.descriptor
                            ?.fqNameSafe ?: clazz.properties
                            .singleOrNull { it.name.asString() == parameter.name }
                            ?.descriptor?.fqNameSafe
                    )
                }

            val assistedParameters = constructorParameters
                .filter { it.assisted }

            val factoryKey = pluginContext.tmpFunction(assistedParameters.size)
                .typeWith(assistedParameters.map { it.type } +
                        clazz.defaultType.typeWith(*typeParametersMap.values.toTypedArray()))
                .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                .asKey()

            if (factoryKey != requestedKey) return emptyList()

            listOf(
                AssistedProvisionBindingNode(
                    key = requestedKey,
                    context = null,
                    dependencies = dependencies,
                    targetScope = scope?.defaultType,
                    scoped = scope != null,
                    module = null,
                    createExpression = newInstanceExpression(
                        clazz,
                        constructor,
                        constructorParameters
                    ),
                    parameters = constructorParameters,
                    owner = factory,
                    origin = clazz.descriptor.fqNameSafe
                )
            )
        } else {
            val clazz = requestedKey.type.classOrNull?.owner ?: return emptyList()
            val constructor = clazz.getInjectConstructor()

            if (constructor?.valueParameters?.any {
                    it.descriptor.hasAnnotation(InjektFqNames.Assisted)
                } == true) return emptyList()

            val scope = clazz.getClassFromSingleValueAnnotationOrNull(
                InjektFqNames.Scoped, pluginContext
            )

            if (scope == null &&
                !clazz.hasAnnotation(InjektFqNames.Unscoped) &&
                constructor?.hasAnnotation(InjektFqNames.Unscoped) != true
            ) return emptyList()

            val typeParametersMap = clazz
                .typeParameters
                .map { it.symbol }
                .associateWith { requestedKey.type.typeArguments[it.owner.index].typeOrFail }

            val parametersNameProvider = NameProvider()

            val constructorParameters = constructor?.valueParameters?.map { valueParameter ->
                InjektDeclarationIrBuilder.FactoryParameter(
                    name = parametersNameProvider.allocateForGroup(valueParameter.name).asString(),
                    type = valueParameter.type,
                    assisted = valueParameter.descriptor.hasAnnotation(InjektFqNames.Assisted)
                )
            } ?: emptyList()

            val dependencies = constructorParameters
                .filterNot { it.assisted }
                .map { parameter ->
                    BindingRequest(
                        key = parameter.type
                            .substituteAndKeepQualifiers(typeParametersMap)
                            .asKey(),
                        requestingKey = requestedKey,
                        requestOrigin = constructor?.valueParameters
                            ?.singleOrNull { it.name.asString() == parameter.name }
                            ?.descriptor
                            ?.fqNameSafe ?: clazz.properties
                            .singleOrNull { it.name.asString() == parameter.name }
                            ?.descriptor?.fqNameSafe
                    )
                }

            listOf(
                ProvisionBindingNode(
                    key = requestedKey,
                    context = null,
                    dependencies = dependencies,
                    targetScope = scope?.defaultType,
                    scoped = scope != null,
                    module = null,
                    createExpression = newInstanceExpression(
                        clazz,
                        constructor,
                        constructorParameters
                    ),
                    parameters = constructorParameters,
                    owner = factory,
                    origin = clazz.fqNameForIrSerialization
                )
            )
        }
    }

    private fun newInstanceExpression(
        clazz: IrClass,
        constructor: IrConstructor?,
        constructorParameters: List<InjektDeclarationIrBuilder.FactoryParameter>
    ): IrBuilderWithScope.(Map<InjektDeclarationIrBuilder.FactoryParameter, () -> IrExpression?>) -> IrExpression {
        return { parametersMap ->
            if (clazz.kind == ClassKind.OBJECT) {
                irGetObject(clazz.symbol)
            } else {
                irCall(constructor!!).apply {
                    constructorParameters
                        .forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                parametersMap.getValue(parameter)()
                            )
                        }
                }
            }
        }
    }
}

class MapBindingResolver(
    private val pluginContext: IrPluginContext,
    private val factory: FactoryImpl,
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
                pluginContext.tmpFunction(0)
                    .typeWith(mapKey.type.typeArguments[1].typeOrFail)
                    .withNoArgAnnotations(pluginContext, listOf(qualifier))
            )
            .withAnnotations(mapKey.type.annotations)
            .asKey(),
        factory,
        map.origin,
        map.entries
    )
}


class SetBindingResolver(
    private val pluginContext: IrPluginContext,
    private val factory: FactoryImpl,
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
                        factory,
                        set.origin,
                        set.elements
                    ),
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
                pluginContext.tmpFunction(0).typeWith(
                    setKey.type.typeArguments.single().typeOrFail
                ).withNoArgAnnotations(pluginContext, listOf(qualifier))
            )
            .withAnnotations(setKey.type.annotations)
            .asKey(),
        factory,
        set.origin,
        set.elements
    )
}

class NoArgProviderBindingResolver(
    private val factory: FactoryImpl
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val requestedType = requestedKey.type
        return when {
            requestedType.isNoArgProvider() ->
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
    private val factoryNode: FactoryNode
) : BindingResolver {
    private val factorySuperClassKey =
        factoryNode.key.type.classOrNull!!.superTypes().single()
            .asKey()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (requestedKey != factorySuperClassKey &&
            requestedKey != factoryNode.key
        ) return emptyList()
        return listOf(
            FactoryImplementationBindingNode(
                factoryNode
            )
        )
    }
}
