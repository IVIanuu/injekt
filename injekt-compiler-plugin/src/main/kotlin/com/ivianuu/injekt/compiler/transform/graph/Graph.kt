package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.ClassKey
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.IntKey
import com.ivianuu.injekt.compiler.LongKey
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.StringKey
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.transform.TopLevelFactoryTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Graph(
    val parent: Graph?,
    val factoryImplementation: FactoryImplementation,
    val factoryTransformer: TopLevelFactoryTransformer,
    val factoryMembers: FactoryMembers,
    factoryImplementationNode: FactoryImplementationNode,
    context: IrPluginContext,
    factoryImplementationModule: ModuleNode?,
    declarationStore: InjektDeclarationStore,
    val symbols: InjektSymbols
) {

    private val scopes = mutableSetOf<FqName>()

    private val explicitBindingResolvers = mutableListOf<BindingResolver>()
    private val implicitBindingResolvers = mutableListOf<BindingResolver>()
    private val mapBindingResolver = MapBindingResolver(context, symbols, factoryImplementation)
    private val setBindingResolver = SetBindingResolver(context, symbols, factoryImplementation)
    private val resolvedBindings = mutableMapOf<Key, BindingNode>()

    init {
        if (factoryImplementationModule != null) addModule(factoryImplementationModule)
        implicitBindingResolvers += LazyOrProviderBindingResolver(symbols, factoryImplementation)
        implicitBindingResolvers += mapBindingResolver
        implicitBindingResolvers += setBindingResolver
        implicitBindingResolvers += MembersInjectorBindingResolver(
            symbols,
            declarationStore,
            factoryImplementation
        )
        implicitBindingResolvers += FactoryImplementationBindingResolver(
            factoryImplementationNode
        )
        implicitBindingResolvers += AnnotatedClassBindingResolver(
            context,
            declarationStore,
            symbols,
            factoryImplementation
        )
    }

    fun getBinding(key: Key): BindingNode {
        var binding = resolvedBindings[key]
        if (binding != null) return binding

        val explicitBindings = explicitBindingResolvers.flatMap { it(key) }
        if (explicitBindings.size > 1) {
            error("Multiple bindings found for $key")
        }

        binding = explicitBindings.singleOrNull()

        if (binding == null) {
            val implicitBindings = implicitBindingResolvers.flatMap { it(key) }
            binding = implicitBindings.singleOrNull()
            if (binding?.targetScope != null && binding.targetScope !in scopes) {
                error(
                    "Scope mismatch binding ${binding.key} " +
                            "with scope ${binding.targetScope} is not compatible with this component $scopes"
                )
            }
        }

        binding?.let { resolvedBindings[key] = it }

        return binding ?: parent?.getBinding(key) ?: error("No binding found for $key")
    }

    private fun addModule(moduleNode: ModuleNode) {
        val module = moduleNode.module

        val descriptor = module.declarations.single {
            it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
        } as IrClass

        val functions = descriptor.functions

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstScope) }
            .forEach { addScope(it.returnType.classOrFail.descriptor.fqNameSafe) }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstDependency) }
            .forEach { function ->
                val field = module.fields
                    .single { field ->
                        field.name.asString() == function.getAnnotation(InjektFqNames.AstFieldPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }
                            .value
                    }
                addExplicitBindingResolver(
                    DependencyBindingResolver(
                        injektTransformer = factoryTransformer,
                        dependencyNode = DependencyNode(
                            dependency = function.returnType.classOrFail.owner,
                            key = Key(function.returnType),
                            initializerAccessor = moduleNode.initializerAccessor.child(field)
                        ),
                        members = factoryMembers,
                        factoryImplementation = factoryImplementation
                    )
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstMap) }
            .forEach { function ->
                addMap(Key(function.returnType))
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstMapEntry) }
            .forEach { function ->
                putMapEntry(
                    Key(function.valueParameters[0].type),
                    function.valueParameters[1].let { entry ->
                        when {
                            entry.hasAnnotation(InjektFqNames.AstMapClassKey) -> {
                                ClassKey(
                                    (entry.getAnnotation(InjektFqNames.AstMapClassKey)
                                    !!.getValueArgument(0) as IrClassReference)
                                        .classType
                                )
                            }
                            entry.hasAnnotation(InjektFqNames.AstMapIntKey) -> {
                                IntKey(
                                    (entry.getAnnotation(InjektFqNames.AstMapIntKey)
                                    !!.getValueArgument(0)!! as IrConst<Int>)
                                        .value
                                )
                            }
                            entry.hasAnnotation(InjektFqNames.AstMapLongKey) -> {
                                LongKey(
                                    (entry.getAnnotation(InjektFqNames.AstMapLongKey)
                                    !!.getValueArgument(0)!! as IrConst<Long>)
                                        .value
                                )
                            }
                            entry.hasAnnotation(InjektFqNames.AstMapStringKey) -> {
                                StringKey(
                                    (entry.getAnnotation(InjektFqNames.AstMapStringKey)
                                    !!.getValueArgument(0)!! as IrConst<String>)
                                        .value
                                )
                            }
                            else -> error("Corrupt map binding ${entry.dump()}")
                        }
                    },
                    Key(function.valueParameters[1].type)
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSet) }
            .forEach { function ->
                addSet(Key(function.returnType))
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSetElement) }
            .forEach { function ->
                addSetElement(
                    Key(function.valueParameters[0].type),
                    Key(function.valueParameters[1].type)
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstModule) }
            .map { it to it.returnType.classOrNull?.owner as IrClass }
            .forEach { (function, includedModule) ->
                val field = module.fields
                    .single { field ->
                        field.name.asString() == function.getAnnotation(InjektFqNames.AstFieldPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }
                            .value
                    }
                addModule(
                    ModuleNode(
                        includedModule,
                        Key(includedModule.defaultType),
                        moduleNode.initializerAccessor.child(field)
                    )
                )
            }

        addExplicitBindingResolver(
            ModuleBindingResolver(
                moduleNode,
                descriptor,
                symbols,
                factoryImplementation
            )
        )

        addExplicitBindingResolver(
            ChildFactoryBindingResolver(
                factoryTransformer,
                factoryImplementation,
                descriptor,
                symbols,
                factoryMembers
            )
        )
    }

    private fun addScope(scope: FqName) {
        scopes += scope
    }

    private fun addExplicitBindingResolver(bindingResolver: BindingResolver) {
        explicitBindingResolvers += bindingResolver
    }

    private fun addMap(key: Key) {
        mapBindingResolver.addMap(key)
    }

    private fun putMapEntry(
        mapKey: Key,
        entryKey: MapKey,
        entryValue: Key
    ) {
        mapBindingResolver.putMapEntry(mapKey, entryKey, DependencyRequest(entryValue))
    }

    private fun addSet(key: Key) {
        setBindingResolver.addSet(key)
    }

    private fun addSetElement(setKey: Key, elementKey: Key) {
        setBindingResolver.addSetElement(setKey, DependencyRequest(elementKey))
    }
}
