package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

typealias FactoryExpression = IrBuilderWithScope.(IrExpression) -> IrExpression

class FactoryExpressions(
    private val symbols: InjektSymbols,
    private val members: FactoryMembers
) {

    // todo remove this circular dep
    lateinit var graph: Graph

    private val bindingExpressions = mutableMapOf<BindingRequest, FactoryExpression>()
    private val chain = mutableSetOf<BindingRequest>()

    private val requirementExpressions = mutableMapOf<RequirementNode, FactoryExpression>()

    fun getRequirementExpression(node: RequirementNode): FactoryExpression {
        requirementExpressions[node]?.let { return it }
        val field = members.getOrCreateComponentField(node.key, node.prefix) fieldInit@{ parent ->
            node.initializerAccessor(this, parent)
        }
        val expression: FactoryExpression = { irGetField(it, field.field) }
        requirementExpressions[node] = expression
        return expression
    }

    fun getBindingExpression(request: BindingRequest): FactoryExpression {
        bindingExpressions[request]?.let { return it }

        check(request !in chain) {
            "Circular dep $request"
        }

        chain += request

        val binding = graph.getBinding(request.key)
        val expression = when (request.requestType) {
            RequestType.Instance -> {
                when (binding) {
                    is InstanceBindingNode -> instanceExpressionForInstance(binding)
                    is ProvisionBindingNode -> instanceExpressionForProvision(binding)
                }
            }
            RequestType.Provider -> {
                when (binding) {
                    is InstanceBindingNode -> providerExpressionForInstance(binding)
                    is ProvisionBindingNode -> providerExpressionForProvision(binding)
                }
            }
        }

        chain -= request

        bindingExpressions[request] = expression
        return expression
    }

    private fun instanceExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return getRequirementExpression(binding.requirementNode)
    }

    private fun instanceExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val expression = if (binding.scoped) {
            val providerExpression = providerExpressionForProvision(binding)
            val expression: FactoryExpression = bindingExpression@{ parent ->
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
            val provider = binding.provider

            val dependencies = binding.dependencies
                .map { getBindingExpression(BindingRequest(it, RequestType.Instance)) }

            val moduleRequired =
                provider.kind != ClassKind.OBJECT && provider.constructors
                    .single().valueParameters.firstOrNull()?.name?.asString() == "module"

            val moduleExpression = if (moduleRequired) getRequirementExpression(binding.module!!)
            else null

            val expression: FactoryExpression = bindingExpression@{ parent ->
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

                    irCall(createFunction).apply {
                        dispatchReceiver = irGetObject(companion.symbol)

                        if (moduleRequired) {
                            putValueArgument(
                                0,
                                moduleExpression!!(
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

        val function = members.getGetFunction(binding.key) {
            expression(this, irGet(it.dispatchReceiverParameter!!))
        }
        return bindingExpression@{
            irCall(function).apply {
                dispatchReceiver = it
            }
        }
    }

    private fun providerExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        val field = members.getOrCreateComponentField(
            Key(
                symbols.provider.typeWith(binding.key.type).buildSimpleType {
                    annotations += binding.key.type.annotations
                }
            ),
            "provider"
        ) { parent ->
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
                    binding.requirementNode
                        .initializerAccessor(this@getOrCreateComponentField, parent)
                )
            }
        }
        return { irGetField(it, field.field) }
    }

    private fun providerExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val dependencyKeys = binding.dependencies
            .map {
                Key(symbols.provider.typeWith(it.type).buildSimpleType {
                    annotations += it.type.annotations
                })
            }

        val dependencies = binding.dependencies
            .map { getBindingExpression(BindingRequest(it, RequestType.Provider)) }

        val field = members.getOrCreateComponentField(
            Key(
                symbols.provider.typeWith(binding.key.type).buildSimpleType {
                    annotations += binding.key.type.annotations
                }
            ),
            "provider"
        ) fieldInit@{ parent ->
            val provider = binding.provider

            val moduleRequired =
                provider.constructors.single().valueParameters.firstOrNull()
                    ?.name?.asString() == "module"

            if (dependencyKeys.any { it !in members.initializedFields }) return@fieldInit null

            val newProvider = irCall(provider.constructors.single()).apply {
                if (moduleRequired) {
                    putValueArgument(
                        0,
                        binding.module!!.initializerAccessor(this@fieldInit, parent)
                    )
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
