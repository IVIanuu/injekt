package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

typealias BindingExpression = IrBuilderWithScope.(IrExpression) -> IrExpression

class FactoryExpressions(
    private val context: IrPluginContext,
    private val symbols: InjektSymbols,
    private val graph: Graph,
    private val componentNode: ComponentNode
) {

    private val bindingExpressions = mutableMapOf<BindingRequest, BindingExpression>()
    private val chain = mutableSetOf<BindingRequest>()

    fun getBindingExpression(request: BindingRequest): BindingExpression {
        bindingExpressions[request]?.let { return it }

        check(request !in chain) {
            "Circular dep $request"
        }

        chain += request

        val binding = graph.getBinding(request)
        val expression = when (request.requestType) {
            RequestType.Instance -> {
                when (binding) {
                    is InstanceBinding -> instanceExpressionForInstance(binding)
                    is ProvisionBinding -> instanceExpressionForProvision(binding)
                }
            }
            RequestType.Provider -> {
                when (binding) {
                    is InstanceBinding -> providerExpressionForInstance(binding)
                    is ProvisionBinding -> providerExpressionForProvision(binding)
                }
            }
        }

        chain -= request

        bindingExpressions[request] = expression
        return expression
    }

    private fun instanceExpressionForInstance(binding: InstanceBinding): BindingExpression {
        return { binding.treeElement(this, it) }
    }

    private fun instanceExpressionForProvision(binding: ProvisionBinding): BindingExpression {
        val expression = if (binding.scoped) {
            val providerExpression = providerExpressionForProvision(binding)
            val expression: BindingExpression = bindingExpression@{ parent ->
                irCall(
                    symbols.provider
                        .owner
                        .declarations
                        .single { it.nameForIrSerialization.asString() == "invoke" } as IrFunction
                ).apply {
                    dispatchReceiver = providerExpression(this@bindingExpression, parent)
                }
            }
            expression
        } else {
            val dependencies = binding.dependencies
                .map { getBindingExpression(BindingRequest(it, RequestType.Instance)) }
            val expression: BindingExpression = bindingExpression@{ parent ->
                val provider = binding.provider
                val createFunction = (if (provider.kind == ClassKind.OBJECT)
                    provider else provider.declarations
                    .single { it.nameForIrSerialization.asString() == "Companion" } as IrClass)
                    .functions
                    .single { it.name.asString() == "create" }

                if (provider.kind == ClassKind.OBJECT) {
                    irCall(
                        provider
                            .functions
                            .single { it.name.asString() == "create" }
                    ).apply {
                        dispatchReceiver = irGetObject(provider.symbol)
                    }
                } else {
                    val companion = provider.companionObject()!! as IrClass

                    val moduleRequired =
                        provider.constructors.single().valueParameters.firstOrNull()
                            ?.name?.asString() == "module"

                    irCall(createFunction).apply {
                        dispatchReceiver = irGetObject(companion.symbol)

                        if (moduleRequired) {
                            putValueArgument(
                                0,
                                binding.module!!.treeElement(
                                    this@bindingExpression,
                                    parent
                                )
                            )
                        }

                        createFunction.valueParameters
                            .drop(if (moduleRequired) 1 else 0)
                            .forEach { valueParameter ->
                                val dependencyExpression =
                                    dependencies[valueParameter.index - if (moduleRequired) 1 else 0]
                                putValueArgument(
                                    valueParameter.index,
                                    dependencyExpression(
                                        this@bindingExpression,
                                        parent.deepCopyWithVariables()
                                    )
                                )
                            }
                    }
                }
            }
            expression
        }

        val function = componentNode.getFunction(binding.key) {
            expression(this, irGet(it.dispatchReceiverParameter!!))
        }
        return bindingExpression@{
            irCall(function).apply {
                dispatchReceiver = it
            }
        }
    }

    private fun providerExpressionForInstance(binding: InstanceBinding): BindingExpression {
        val field = componentNode.getOrCreateComponentField(binding.key) {
            val companion = symbols.instanceProvider.owner
                .companionObject() as IrClass
            irCall(
                companion
                    .declarations
                    .filterIsInstance<IrFunction>()
                    .single { it.name.asString() == "create" }
            ).apply {
                dispatchReceiver = irGetObject(companion.symbol)
                putValueArgument(
                    0,
                    binding.treeElement(this@getOrCreateComponentField, it)
                )
            }
        }
        return { irGetField(it, field.field) }
    }

    private fun providerExpressionForProvision(binding: ProvisionBinding): BindingExpression {
        val dependencies = binding.dependencies
            .map { getBindingExpression(BindingRequest(it, RequestType.Provider)) }

        val field = componentNode.getOrCreateComponentField(binding.key) fieldInit@{ parent ->
            val provider = binding.provider

            val moduleRequired =
                provider.constructors.single().valueParameters.firstOrNull()
                    ?.name?.asString() == "module"

            if (binding.dependencies.any { it !in componentNode.initializedFields }) return@fieldInit null

            val newProvider = irCall(provider.constructors.single()).apply {
                if (moduleRequired) {
                    putValueArgument(0, binding.module!!.treeElement(this@fieldInit, parent))
                }

                dependencies.forEachIndexed { index, dependency ->
                    val realIndex = index + if (moduleRequired) 1 else 0
                    putValueArgument(
                        realIndex,
                        dependency(this@fieldInit, parent.deepCopyWithVariables())
                    )
                }
            }

            if (binding.scoped) {
                irCall(
                    symbols.doubleCheck
                        .constructors
                        .single()
                ).apply { putValueArgument(0, newProvider) }
            } else {
                newProvider
            }
        }

        return bindingExpression@{ irGetField(it, field.field) }
    }

}
