package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.ClassKey
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.IntKey
import com.ivianuu.injekt.compiler.LongKey
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.StringKey
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.findPropertyGetter
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.substituteByName
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.type
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Graph(
    val parent: Graph?,
    val factoryProduct: AbstractFactoryProduct,
    val factoryMembers: FactoryMembers,
    context: IrPluginContext,
    factoryImplementationModule: ModuleNode?,
    declarationStore: InjektDeclarationStore,
    val symbols: InjektSymbols
) {

    private val scopes = mutableSetOf<FqName>()

    private val explicitBindingResolvers = mutableListOf<BindingResolver>()
    private val implicitBindingResolvers = mutableListOf<BindingResolver>()
    val mapBindingResolver: MapBindingResolver =
        MapBindingResolver(
            context,
            symbols,
            factoryProduct,
            parent?.mapBindingResolver
        )
    val setBindingResolver: SetBindingResolver =
        SetBindingResolver(
            context,
            symbols,
            factoryProduct,
            parent?.setBindingResolver
        )
    private val resolvedBindings = mutableMapOf<Key, BindingNode>()

    private val chain = mutableSetOf<Key>()

    init {
        if (factoryImplementationModule != null) addModule(factoryImplementationModule)
        implicitBindingResolvers += LazyOrProviderBindingResolver(
            symbols,
            factoryProduct
        )
        implicitBindingResolvers += mapBindingResolver
        implicitBindingResolvers += setBindingResolver
        implicitBindingResolvers += MembersInjectorBindingResolver(
            symbols,
            declarationStore,
            factoryProduct
        )
        if (factoryProduct is FactoryImplementation) {
            implicitBindingResolvers += FactoryImplementationBindingResolver(
                factoryProduct.factoryImplementationNode
            )
        }
        implicitBindingResolvers += AnnotatedClassBindingResolver(
            context,
            declarationStore,
            symbols,
            factoryProduct
        )
    }

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        check(request.key !in chain) {
            "Circular dependency ${request.key}"
        }
        chain += request.key

        val explicitBindings = explicitBindingResolvers.flatMap { it(request.key) }
        if (explicitBindings.size > 1) {
            error(
                "Multiple bindings found for '${request.key}' at:\n${explicitBindings.joinToString(
                    "\n"
                ) {
                    "'${it.origin.orUnknown()}'"
                }}"
            )
        }

        binding = explicitBindings.singleOrNull()

        if (binding == null) {
            val implicitBindings = implicitBindingResolvers.flatMap { it(request.key) }
            binding = implicitBindings.singleOrNull()
            if (binding?.targetScope != null && binding.targetScope !in scopes) {
                if (parent == null) {
                    error(
                        "Scope mismatch binding '${binding.key}' " +
                                "with scope '${binding.targetScope}' is not compatible with this component ${scopes.map { "'$it'" }}"
                    )
                } else {
                    binding = null
                }
            }
        }

        binding?.let { resolvedBindings[request.key] = it }

        chain -= request.key

        return binding ?: parent?.getBinding(request)
        ?: error("No binding found for '${request.key}' required at '${request.requestOrigin.orUnknown()}'")
    }

    fun validate(keys: List<BindingRequest>) {
        keys.forEach {
            val binding = getBinding(it)
            validate(binding.dependencies)
        }
    }

    private fun addModule(moduleNode: ModuleNode) {
        val module = moduleNode.module

        val descriptor = module.declarations.single {
            it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
        } as IrClass

        val functions = descriptor.functions.toList()

        functions
            .flatMap { it.annotations }
            .forEach { it.symbol.ensureBound(factoryProduct.pluginContext.irProviders) }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstScope) }
            .forEach { addScope(it.returnType.classOrFail.descriptor.fqNameSafe) }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstDependency) }
            .forEach { function ->
                val dependencyName = function.getAnnotation(InjektFqNames.AstPropertyPath)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value
                val dependencyType = function.returnType
                    .substituteByName(moduleNode.typeParametersMap)
                addExplicitBindingResolver(
                    DependencyBindingResolver(
                        moduleNode = moduleNode,
                        dependencyNode = DependencyNode(
                            dependency = function.returnType.classOrFail.owner,
                            key = dependencyType.asKey(factoryProduct.pluginContext),
                            initializerAccessor = moduleNode.initializerAccessor.child(
                                moduleNode.module.findPropertyGetter(dependencyName)
                            )
                        ),
                        members = factoryMembers,
                        factoryProduct = factoryProduct
                    )
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstMap) }
            .forEach { function ->
                addMap(
                    key = function
                        .returnType
                        .substituteByName(moduleNode.typeParametersMap)
                        .asKey(factoryProduct.pluginContext),
                    origin = module.descriptor.fqNameSafe
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstMapEntry) }
            .forEach { function ->
                val entryKey = function.valueParameters[1].let { entry ->
                    val entryDescriptor = entry.descriptor
                    when {
                        entryDescriptor.annotations.hasAnnotation(InjektFqNames.AstMapClassKey) -> {
                            ClassKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapClassKey)
                                !!.allValueArguments.values.single())
                                    .let { it as KClassValue }
                                    .getArgumentType(factoryProduct.pluginContext.moduleDescriptor)
                                    .let {
                                        factoryProduct.pluginContext.typeTranslator.translateType(
                                            it
                                        )
                                    }
                            )
                        }
                        entryDescriptor.annotations.hasAnnotation(InjektFqNames.AstMapTypeParameterClassKey) -> {
                            ClassKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapTypeParameterClassKey)
                                !!.allValueArguments.values.single())
                                    .let { it as StringValue }
                                    .value
                                    .let { typeParameterName ->
                                        moduleNode.typeParametersMap.toList()
                                            .filter { it.first.descriptor.name.asString() == typeParameterName }
                                            .single()
                                            .second
                                    }
                            )
                        }
                        entryDescriptor.annotations.hasAnnotation(InjektFqNames.AstMapIntKey) -> {
                            IntKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapIntKey)
                                !!.allValueArguments.values.single())
                                    .let { it as IntValue }
                                    .value
                            )
                        }
                        entryDescriptor.annotations.hasAnnotation(InjektFqNames.AstMapLongKey) -> {
                            LongKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapLongKey)
                                !!.allValueArguments.values.single())
                                    .let { it as LongValue }
                                    .value
                            )
                        }
                        entryDescriptor.annotations.hasAnnotation(InjektFqNames.AstMapStringKey) -> {
                            StringKey(
                                (entry.descriptor.annotations.findAnnotation(InjektFqNames.AstMapStringKey)
                                !!.allValueArguments.values.single())
                                    .let { it as StringValue }
                                    .value
                            )
                        }
                        else -> error("Corrupt map binding ${function.dump()}")
                    }
                }
                putMapEntry(
                    mapKey = function.valueParameters[0].type
                        .substituteByName(moduleNode.typeParametersMap)
                        .asKey(factoryProduct.pluginContext),
                    entryKey = entryKey,
                    entryValue = function.valueParameters[1].type
                        .substituteByName(moduleNode.typeParametersMap)
                        .asKey(factoryProduct.pluginContext),
                    origin = module.descriptor.fqNameSafe
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSet) }
            .forEach { function ->
                addSet(
                    key = function.returnType
                        .substituteByName(moduleNode.typeParametersMap)
                        .asKey(factoryProduct.pluginContext),
                    origin = module.descriptor.fqNameSafe
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSetElement) }
            .forEach { function ->
                addSetElement(
                    setKey = function.valueParameters[0].type
                        .substituteByName(moduleNode.typeParametersMap)
                        .asKey(factoryProduct.pluginContext),
                    elementKey = function.valueParameters[1].type
                        .substituteByName(moduleNode.typeParametersMap)
                        .asKey(factoryProduct.pluginContext),
                    origin = module.descriptor.fqNameSafe
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstModule) }
            .map {
                it to it.returnType
                    .substituteByName(moduleNode.typeParametersMap)
                    .classOrNull?.owner as IrClass
            }
            .forEach { (function, includedModule) ->
                val moduleName = function.getAnnotation(InjektFqNames.AstPropertyPath)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value

                val property = moduleNode.module.findPropertyGetter(moduleName)

                val typeParametersMap = includedModule.typeParameters
                    .map { it.symbol to (property.returnType as IrSimpleType).arguments[it.index].type }
                    .toMap()
                    .mapValues { it.value.substituteByName(moduleNode.typeParametersMap) }

                addModule(
                    ModuleNode(
                        includedModule,
                        includedModule.defaultType
                            .substituteByName(moduleNode.typeParametersMap)
                            .asKey(factoryProduct.pluginContext),
                        moduleNode.initializerAccessor.child(property),
                        typeParametersMap
                    )
                )
            }

        addExplicitBindingResolver(
            ModuleBindingResolver(
                moduleNode,
                descriptor,
                symbols,
                factoryProduct
            )
        )

        if (factoryProduct is FactoryImplementation) {
            addExplicitBindingResolver(
                ChildFactoryBindingResolver(
                    factoryProduct,
                    descriptor,
                    symbols,
                    factoryMembers
                )
            )
        }
    }

    private fun addScope(scope: FqName) {
        scopes += scope
    }

    private fun addExplicitBindingResolver(bindingResolver: BindingResolver) {
        explicitBindingResolvers += bindingResolver
    }

    private fun addMap(key: Key, origin: FqName) {
        mapBindingResolver.addMap(key, origin)
    }

    private fun putMapEntry(
        mapKey: Key,
        entryKey: MapKey,
        entryValue: Key,
        origin: FqName
    ) {
        mapBindingResolver.putMapEntry(
            mapKey, entryKey,
            BindingRequest(entryValue, origin)
        )
    }

    private fun addSet(key: Key, origin: FqName) {
        setBindingResolver.addSet(key, origin)
    }

    private fun addSetElement(
        setKey: Key,
        elementKey: Key,
        origin: FqName
    ) {
        setBindingResolver.addSetElement(
            setKey,
            BindingRequest(elementKey, origin)
        )
    }
}
