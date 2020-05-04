package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getQualifiers
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeWith
import com.ivianuu.injekt.compiler.withQualifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias BindingResolver = (Key) -> List<BindingNode>

class DependencyBindingResolver(
    private val injektTransformer: AbstractInjektTransformer,
    private val dependencyNode: DependencyNode,
    private val expressions: FactoryExpressions,
    private val members: FactoryMembers
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
                    it.dispatchReceiverParameter!!.type != injektTransformer.irBuiltIns.anyType
        }

    private val providersByDependency = mutableMapOf<IrFunction, IrClass>()

    private fun provider(dependencyFunction: IrFunction): IrClass =
        providersByDependency.getOrPut(dependencyFunction) {
            with(injektTransformer) {
                with(DeclarationIrBuilder(injektTransformer.context, dependencyFunction.symbol)) {
                    provider(
                        name = Name.identifier("dep_provider_${providersByDependency.size}"),
                        parameters = listOf(
                            AbstractInjektTransformer.ProviderParameter(
                                name = "dependency",
                                type = dependencyNode.dependency.defaultType,
                                assisted = false
                            )
                        ),
                        returnType = dependencyFunction.returnType,
                        createBody = { createFunction ->
                            irExprBody(
                                irCall(dependencyFunction).apply {
                                    dispatchReceiver =
                                        irGet(createFunction.valueParameters.single())
                                }
                            )
                        }
                    ).also { members.addClass(it) }
                }
            }
        }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return allDependencyFunctions
            .filter { it.returnType.asKey() == requestedKey }
            .map { dependencyFunction ->
                val provider = provider(dependencyFunction)
                DependencyBindingNode(
                    key = requestedKey,
                    provider = provider,
                    requirementNode = dependencyNode
                )
            }
    }
}

class ModuleBindingResolver(
    private val moduleNode: ModuleNode,
    private val descriptor: IrClass,
    private val symbols: InjektSymbols
) : BindingResolver {

    private val bindingFunctions = descriptor
        .declarations
        .filterIsInstance<IrFunction>()

    private val allBindings = bindingFunctions
        .filter { it.hasAnnotation(InjektFqNames.AstBinding) }
        .map { bindingFunction ->
            val bindingKey = bindingFunction.returnType.asKey()
            val fieldName = bindingFunction.getAnnotation(InjektFqNames.AstFieldPath)
                ?.getValueArgument(0)?.let { it as IrConst<String> }?.value
            val provider = bindingFunction.getAnnotation(InjektFqNames.AstClassPath)
                ?.getTypeArgument(0)?.classOrNull?.owner

            val scoped = bindingFunction.hasAnnotation(InjektFqNames.AstScoped)

            when {
                fieldName != null -> {
                    val field =
                        moduleNode.module.fields.single { it.name.asString() == fieldName }
                    InstanceBindingNode(
                        key = bindingKey,
                        requirementNode = InstanceNode(
                            key = field.type.asKey(),
                            initializerAccessor = moduleNode.initializerAccessor.child(field)
                        )
                    )
                }
                else -> {
                    if (bindingFunction.valueParameters.any { it.hasAnnotation(InjektFqNames.AstAssisted) }) {
                        val (assistedValueParameters, nonAssistedValueParameters) = bindingFunction.valueParameters
                            .partition { it.hasAnnotation(InjektFqNames.AstAssisted) }

                        val assistedFactoryType = symbols.getFunction(assistedValueParameters.size)
                            .typeWith(
                                assistedValueParameters
                                    .map { it.type } + bindingKey.type
                            ).withQualifiers(symbols, listOf(InjektFqNames.Provider))

                        val dependencies = bindingFunction.valueParameters
                            .filterNot { it.hasAnnotation(InjektFqNames.AstAssisted) }
                            .map { it.type.asKey() }
                            .map { DependencyRequest(it) }

                        AssistedProvisionBindingNode(
                            key = Key(assistedFactoryType),
                            dependencies = dependencies,
                            targetScope = null,
                            scoped = scoped,
                            module = moduleNode,
                            provider = provider!!
                        )
                    } else {
                        val dependencies = bindingFunction.valueParameters
                            .map { it.type.asKey() }
                            .map { DependencyRequest(it) }

                        ProvisionBindingNode(
                            key = bindingKey,
                            dependencies = dependencies,
                            targetScope = null,
                            scoped = scoped,
                            module = moduleNode,
                            provider = provider!!
                        )
                    }
                }
            }
        }

    private val delegateBindings = bindingFunctions
        .filter { it.hasAnnotation(InjektFqNames.AstAlias) }
        .map { delegateFunction ->
            DelegateBindingNode(
                key = Key(delegateFunction.returnType),
                originalKey = Key(delegateFunction.valueParameters.single().type)
            )
        }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return (allBindings + delegateBindings)
            .filter { it.key == requestedKey }
    }
}

class MembersInjectorBindingResolver(
    private val symbols: InjektSymbols,
    private val declarationStore: InjektDeclarationStore
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (InjektFqNames.MembersInjector !in requestedKey.type.getQualifiers()) return emptyList()
        if (requestedKey.type.classOrNull != symbols.getFunction(1)) return emptyList()
        val target = requestedKey.type.typeArguments.first().classOrFail.owner
        val membersInjector = declarationStore.getMembersInjector(target)
        return listOf(
            MembersInjectorBindingNode(
                key = requestedKey,
                membersInjector = membersInjector
            )
        )
    }
}

class AnnotatedClassBindingResolver(
    private val context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val clazz = requestedKey.type.classOrNull
            ?.ensureBound(context.irProviders)?.owner ?: return emptyList()
        val scopeAnnotation = clazz.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
            .singleOrNull() ?: return emptyList()
        val provider = declarationStore.getProvider(clazz)

        val constructor = clazz.constructors
            .single()

        val targetScope = scopeAnnotation.fqName?.takeIf { it != InjektFqNames.Transient }

        val scoped = scopeAnnotation.fqName != InjektFqNames.Transient

        if (constructor.valueParameters.any {
                it.hasAnnotation(InjektFqNames.Assisted)
            }) {
            val dependencies = constructor.valueParameters
                .filterNot { it.hasAnnotation(InjektFqNames.Assisted) }
                .map { it.type.asKey() }
                .map { DependencyRequest(it) }

            return listOf(
                AssistedProvisionBindingNode(
                    key = requestedKey,
                    dependencies = dependencies,
                    targetScope = targetScope,
                    scoped = scoped,
                    module = null,
                    provider = provider
                )
            )
        } else {
            val dependencies = constructor.valueParameters
                .map { it.type.asKey() }
                .map { DependencyRequest(it) }

            return listOf(
                ProvisionBindingNode(
                    key = requestedKey,
                    dependencies = dependencies,
                    targetScope = targetScope,
                    scoped = scoped,
                    module = null,
                    provider = provider
                )
            )
        }
    }
}

class MapBindingResolver(
    private val injektTransformer: AbstractInjektTransformer,
    private val context: IrPluginContext,
    private val symbols: InjektSymbols
) : BindingResolver {

    private val maps =
        mutableMapOf<Key, MutableMap<MapKey, DependencyRequest>>()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return maps
            .flatMap { (mapKey, entries) ->
                listOf(
                    MapBindingNode(mapKey, entries),
                    frameworkBinding(InjektFqNames.Lazy, mapKey, entries),
                    frameworkBinding(InjektFqNames.Provider, mapKey, entries)
                )
            }
            .filter { it.key == requestedKey }
    }

    fun addMap(mapKey: Key) {
        maps.getOrPut(mapKey) { mutableMapOf() }
    }

    fun putMapEntry(
        mapKey: Key,
        entryKey: MapKey,
        entryValue: DependencyRequest
    ) {
        val map = maps[mapKey]!!
        if (entryKey in map) {
            error("Already bound value with $entryKey into map $mapKey")
        }

        map[entryKey] = entryValue
    }

    private fun frameworkBinding(
        qualifier: FqName,
        mapKey: Key,
        entries: Map<MapKey, DependencyRequest>
    ) = MapBindingNode(
        context.symbolTable.referenceClass(context.builtIns.map)
            .typeWith(
                mapKey.type.typeArguments[0],
                symbols.getQualifiedFunctionType(
                        0,
                        listOf(qualifier)
                    ).typeWith(mapKey.type.typeArguments[1])
                    .withQualifiers(symbols, listOf(qualifier))
            ).asKey(),
        entries
            .mapValues {
                DependencyRequest(
                    key = symbols.getFunction(0).typeWith(
                        it.value.key.type
                    ).withQualifiers(symbols, listOf(qualifier)).asKey()
                )
            }
    )
}


class SetBindingResolver(
    private val context: IrPluginContext,
    private val symbols: InjektSymbols
) : BindingResolver {

    private val sets = mutableMapOf<Key, MutableSet<DependencyRequest>>()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return sets
            .flatMap { (setKey, elements) ->
                listOf(
                    SetBindingNode(setKey, elements.toList()),
                    frameworkBinding(InjektFqNames.Lazy, setKey, elements),
                    frameworkBinding(InjektFqNames.Provider, setKey, elements)
                )
            }
            .filter { it.key == requestedKey }
    }

    fun addSet(setKey: Key) {
        sets.getOrPut(setKey) { mutableSetOf() }
    }

    fun addSetElement(setKey: Key, element: DependencyRequest) {
        val set = sets[setKey]!!
        if (element in set) {
            error("Already bound $element into set $setKey")
        }

        set += element
    }

    private fun frameworkBinding(
        qualifier: FqName,
        setKey: Key,
        elements: Set<DependencyRequest>
    ) = SetBindingNode(
        Key(
            context.symbolTable.referenceClass(context.builtIns.set)
                .typeWith(
                    symbols.getFunction(0).typeWith(
                        setKey.type.typeArguments.single()
                    ).withQualifiers(symbols, listOf(qualifier))
                )
        ),
        elements
            .map {
                DependencyRequest(
                    key = Key(
                        symbols.getFunction(0).typeWith(
                            it.key.type
                        ).withQualifiers(symbols, listOf(qualifier))
                    )
                )
            }
    )
}

class LazyOrProviderBindingResolver(
    private val symbols: InjektSymbols
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val requestedType = requestedKey.type
        return when {
            requestedType.isFunction() && requestedType.hasAnnotation(InjektFqNames.Lazy) ->
                listOf(LazyBindingNode(key = requestedKey))
            requestedType.isFunction() && requestedType.hasAnnotation(InjektFqNames.Provider) ->
                listOf(ProviderBindingNode(key = requestedKey))
            else -> emptyList()
        }
    }
}

class FactoryImplementationBindingResolver(
    private val factoryImplementationNode: FactoryImplementationNode
) : BindingResolver {
    private val factorySuperClassKey =
        factoryImplementationNode.key.type.classOrFail.superTypes().single().asKey()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (requestedKey != factorySuperClassKey) return emptyList()
        return listOf(
            FactoryImplementationBindingNode(factoryImplementationNode)
        )
    }
}
