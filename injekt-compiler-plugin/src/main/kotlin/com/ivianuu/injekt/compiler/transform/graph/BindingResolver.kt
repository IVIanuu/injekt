package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
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
                        dependencies = mapOf("dependency" to dependencyNode.dependency.defaultType),
                        type = dependencyFunction.returnType,
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
            .filter { Key(it.returnType) == requestedKey }
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
    private val descriptor: IrClass
) : BindingResolver {

    private val bindingFunctions = descriptor
        .declarations
        .filterIsInstance<IrFunction>()

    private val allBindings = bindingFunctions
        .filter { it.hasAnnotation(InjektFqNames.AstBinding) }
        .mapNotNull { bindingFunction ->
            val key = Key(bindingFunction.returnType)
            val fieldName = bindingFunction.getAnnotation(InjektFqNames.AstFieldPath)
                ?.getValueArgument(0)?.let { it as IrConst<String> }?.value
            val provider = bindingFunction.getAnnotation(InjektFqNames.AstClassPath)
                ?.getTypeArgument(0)?.classOrNull?.owner

            // todo handle assisted
            if (bindingFunction.valueParameters.any {
                    it.hasAnnotation(InjektFqNames.Assisted)
                }) return@mapNotNull null

            val scoped = bindingFunction.hasAnnotation(InjektFqNames.AstScoped)

            val dependencies = bindingFunction.valueParameters
                .map { Key(it.type) }
                .map { DependencyRequest(it) }

            when {
                fieldName != null -> {
                    val field =
                        moduleNode.module.fields.single { it.name.asString() == fieldName }
                    InstanceBindingNode(
                        key = key,
                        requirementNode = InstanceNode(
                            key = Key(field.type),
                            initializerAccessor = moduleNode.initializerAccessor.child(field)
                        )
                    )
                }
                else -> {
                    ProvisionBindingNode(
                        key = key,
                        dependencies = dependencies,
                        targetScope = null,
                        scoped = scoped,
                        module = moduleNode,
                        provider = provider!!
                    )
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

        if (constructor.valueParameters.any {
                it.hasAnnotation(InjektFqNames.Assisted)
            }) return emptyList()

        val dependencies = constructor.valueParameters
            .map { Key(it.type) }
            .map { DependencyRequest(it) }

        val targetScope = scopeAnnotation.fqName?.takeIf { it != InjektFqNames.Transient }

        val scoped = scopeAnnotation.fqName != InjektFqNames.Transient

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

class SetBindingResolver : BindingResolver {

    private val sets = mutableMapOf<Key, MutableSet<DependencyRequest>>()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return sets
            .flatMap { (setKey, elementKeys) ->
                listOf(SetBindingNode(setKey, elementKeys.toList()))
            }
            .filter { it.key == requestedKey }
    }

    fun addSet(setKey: Key) {
        sets.getOrPut(setKey) { mutableSetOf() }
    }

    fun addSetElement(setKey: Key, elementKey: Key) {
        val set = sets[setKey]!!
        if (elementKey in set) {
            error("Already bound $elementKey into set $setKey")
        }

        set += DependencyRequest(elementKey)
    }
}

class LazyOrProviderBindingResolver(
    private val symbols: InjektSymbols
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val requestedType = requestedKey.type
        if (requestedType !is IrSimpleType) return emptyList()
        if (requestedType.arguments.size != 1) return emptyList()
        if (requestedType.classifier != symbols.lazy &&
            requestedType.classifier != symbols.provider
        ) return emptyList()

        val dependency = Key(
            requestedType.arguments.single().typeOrNull!!
                .let { it as IrSimpleType }
                .buildSimpleType { annotations += requestedKey.type.annotations }
        ).let { DependencyRequest(it) }

        return when (requestedType.classifier) {
            /*symbols.provider -> {
                DelegateBindingNode(
                    key = requestedKey,
                    dependencies = listOf(dependency),
                    null,
                    false,
                    null,

                )
            }*/
            else -> emptyList()
        }
    }
}

class FactoryImplementationBindingResolver(
    private val factoryImplementationNode: FactoryImplementationNode
) : BindingResolver {
    private val factorySuperClassKey =
        Key(factoryImplementationNode.key.type.classOrNull!!.superTypes().single())

    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (requestedKey != factorySuperClassKey) return emptyList()
        return listOf(
            FactoryImplementationBindingNode(factoryImplementationNode)
        )
    }
}