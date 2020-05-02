package com.ivianuu.injekt.compiler.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation

typealias BindingResolver = (Key) -> List<Binding>

class ModuleBindingResolver(
    private val graph: Graph,
    private val moduleNode: ModuleNode,
    private val module: IrClass,
    private val descriptor: IrClass
) : BindingResolver {

    private val allBindings = descriptor
        .declarations
        .filterIsInstance<IrFunction>()
        .filter { it.hasAnnotation(InjektFqNames.AstBinding) }

    override fun invoke(requestedKey: Key): List<Binding> {
        return allBindings
            .filter { Key(it.returnType) == requestedKey }
            .mapNotNull { bindingFunction ->
                val fieldName = bindingFunction.getAnnotation(InjektFqNames.AstFieldPath)
                    ?.getValueArgument(0)?.let { it as IrConst<String> }?.value
                val provider = bindingFunction.getAnnotation(InjektFqNames.AstClassPath)
                    ?.getTypeArgument(0)?.classOrNull?.owner

                if (bindingFunction.valueParameters.any {
                        it.hasAnnotation(InjektFqNames.Assisted)
                    }) return@mapNotNull null

                val isScoped = bindingFunction.hasAnnotation(InjektFqNames.AstScoped)

                val dependencies = bindingFunction.valueParameters
                    .map { Key(it.type) }

                val moduleRequired =
                    provider?.constructors?.single()?.valueParameters?.firstOrNull()
                        ?.name?.asString() == "module"

                when {
                    fieldName != null -> {
                        val instanceTreeElement = moduleNode.treeElement.child(
                            module.fields.single { it.name.asString() == fieldName }
                        )
                        val providerField = lazy {
                            graph.allocateProviderField(requestedKey.type)
                        }

                        graph.statefulBinding(
                            key = requestedKey,
                            dependencies = emptyList(),
                            providerInstance = {
                                providerField.value

                                irCall(
                                    graph.symbols.instanceProvider
                                        .owner
                                        .companionObject()!!
                                        .let { it as IrClass }
                                        .declarations
                                        .filterIsInstance<IrFunction>()
                                        .single { it.name.asString() == "create" }
                                ).apply {
                                    putValueArgument(
                                        0,
                                        instanceTreeElement(it)
                                    )
                                }
                            },
                            getFunction = graph.treeElementGetFunction(
                                requestedKey,
                                instanceTreeElement
                            ),
                            providerField = { providerField.takeIf { it.isInitialized() }?.value }
                        )
                    }
                    isScoped -> {
                        provider!!
                        val providerField = graph.allocateProviderField(requestedKey.type)

                        graph.statefulBinding(
                            key = requestedKey,
                            dependencies = dependencies,
                            targetScope = null,
                            providerInstance = graph.newProviderInstance(
                                provider,
                                isScoped,
                                if (moduleRequired) moduleNode else null
                            ),
                            getFunction = graph.providerInvokeGetFunction(
                                key = requestedKey,
                                treeElement = graph.thisComponent.treeElement.child(providerField)
                            ),
                            { providerField }
                        )
                    }
                    else -> {
                        provider!!
                        graph.statelessBinding(
                            key = requestedKey,
                            dependencies = dependencies,
                            provider = provider,
                            moduleIfRequired = if (moduleRequired) moduleNode else null
                        )
                    }
                }
            }

    }
}

class AnnotatedClassBindingResolver(
    private val graph: Graph,
    private val context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<Binding> {
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

        val isScoped = scopeAnnotation.fqName != InjektFqNames.Transient

        val binding = if (isScoped) {
            val providerField = graph.allocateProviderField(requestedKey.type)

            graph.statefulBinding(
                key = requestedKey,
                dependencies = dependencies,
                targetScope = scopeAnnotation.fqName!!,
                providerInstance = graph.newProviderInstance(provider, isScoped, null),
                getFunction = graph.providerInvokeGetFunction(
                    key = requestedKey,
                    treeElement = graph.thisComponent.treeElement.child(providerField)
                ),
                providerField = {
                    dependencies
                        .map { graph.requestBinding(it) }
                        .forEach { providerField }// todo

                    providerField
                }
            )
        } else {
            graph.statelessBinding(
                key = requestedKey,
                dependencies = dependencies,
                provider = provider,
                moduleIfRequired = null
            )
        }

        return listOf(binding)
    }
}

class LazyOrProviderBindingResolver(
    private val context: IrPluginContext,
    private val graph: Graph,
) : BindingResolver {
    private val symbols = graph.symbols
    override fun invoke(requestedKey: Key): List<Binding> {
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
        )

        return when (requestedType.classifier) {
            symbols.provider -> {
                Binding(
                    key = requestedKey,
                    dependencies = listOf(dependency),
                    targetScope = null,
                    providerInstance = {
                        graph.requestBinding(dependency)
                            .providerInstance(this, it)
                    },
                    getFunction = graph.getFunction(requestedKey) { function ->
                        graph.requestBinding(dependency)
                            .providerInstance(this, irGet(function.dispatchReceiverParameter!!))
                    },
                    providerField = { null }
                ).let { listOf(it) }
            }
            else -> emptyList()
        }
    }
}
