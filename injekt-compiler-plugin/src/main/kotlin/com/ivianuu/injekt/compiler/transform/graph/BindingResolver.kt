package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation

typealias BindingResolver = (Key) -> List<BindingNode>

class ModuleBindingResolver(
    private val moduleNode: ModuleNode,
    private val module: IrClass,
    private val descriptor: IrClass
) : BindingResolver {

    private val allBindings = descriptor
        .declarations
        .filterIsInstance<IrFunction>()
        .filter { it.hasAnnotation(InjektFqNames.AstBinding) }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return allBindings
            .filter { Key(it.returnType) == requestedKey }
            .mapNotNull { bindingFunction ->
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

                when {
                    fieldName != null -> {
                        val field = module.fields.single { it.name.asString() == fieldName }
                        InstanceBindingNode(
                            key = requestedKey,
                            targetScope = null,
                            scoped = false,
                            module = moduleNode,
                            requirementNode = InstanceRequirementNode(
                                key = Key(field.type),
                                initializerAccessor = moduleNode.initializerAccessor.child(field)
                            )
                        )
                    }
                    else -> {
                        ProvisionBindingNode(
                            key = requestedKey,
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
        )

        /*return when (requestedType.classifier) {
            symbols.provider -> {
                Binding(
                    key = requestedKey,
                    dependencies = listOf(dependency),
                    targetScope = null,
                    providerExpression = {
                        graph.getBinding(dependency)
                            .providerExpression(this, it)
                    },
                    getFunction = graph.getFunction(requestedKey) { function ->
                        graph.getBinding(dependency)
                            .providerExpression(this, irGet(function.dispatchReceiverParameter!!))
                    },
                    providerField = { null }
                ).let { listOf(it) }
            }
            else -> emptyList()
        }*/
        return emptyList()
    }
}
