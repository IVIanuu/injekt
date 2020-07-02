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
import com.ivianuu.injekt.compiler.isNoArgProvider
import com.ivianuu.injekt.compiler.isProvider
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.FqName

class FactoryExpressions(
    private val graph: Graph,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val members: FactoryMembers,
    private val parent: FactoryExpressions?,
    private val factory: FactoryImpl
) {

    private val bindingExpressions = mutableMapOf<BindingRequest, FactoryExpression>()

    private val requirementExpressions = mutableMapOf<RequirementNode, FactoryExpression>()

    private val pair = pluginContext.referenceClass(FqName("kotlin.Pair"))!!
        .owner

    fun getRequirementExpression(node: RequirementNode): FactoryExpression {
        requirementExpressions[node]?.let { return it }
        val expression = members.cachedValue(node.key) {
            node.accessor(this)!!
        }
        requirementExpressions[node] = expression
        return expression
    }

    fun getBindingExpression(request: BindingRequest): FactoryExpression {
        val binding = graph.getBinding(request)

        if (binding.owner != factory) {
            return parent?.getBindingExpression(request)!!
        }

        bindingExpressions[request]?.let { return it }

        val expression = when (request.requestType) {
            RequestType.Instance -> {
                if (bindingExpressions.containsKey(
                        BindingRequest(
                            binding.key,
                            request.requestingKey,
                            binding.origin,
                            RequestType.Provider
                        )
                    )
                ) {
                    if (request.key.type.isProvider()) {
                        getBindingExpression(
                            BindingRequest(
                                binding.key,
                                request.requestingKey,
                                binding.origin,
                                RequestType.Provider
                            )
                        )
                    } else {
                        invokeProviderInstanceExpression(binding)
                    }
                } else {
                    when (binding) {
                        is AssistedProvisionBindingNode -> instanceExpressionForAssistedProvision(
                            binding
                        )
                        is ChildFactoryBindingNode -> instanceExpressionForChildFactory(binding)
                        is DelegateBindingNode -> instanceExpressionForDelegate(binding)
                        is DependencyBindingNode -> instanceExpressionForDependency(binding)
                        is FactoryImplementationBindingNode -> instanceExpressionForFactoryImplementation(
                            binding
                        )
                        is InstanceBindingNode -> instanceExpressionForInstance(binding)
                        is MapBindingNode -> instanceExpressionForMap(binding)
                        is NullBindingNode -> instanceExpressionForNull(binding)
                        is ProviderBindingNode -> instanceExpressionForProvider(binding)
                        is ProvisionBindingNode -> instanceExpressionForProvision(binding)
                        is SetBindingNode -> instanceExpressionForSet(binding)
                    }.wrapInFunction(binding.key)
                }
            }
            RequestType.Provider -> {
                when (binding) {
                    is AssistedProvisionBindingNode -> providerExpressionForAssistedProvision(
                        binding
                    )
                    is ChildFactoryBindingNode -> providerExpressionForChildFactory(binding)
                    is DelegateBindingNode -> providerExpressionForDelegate(binding)
                    is DependencyBindingNode -> providerExpressionForDependency(binding)
                    is FactoryImplementationBindingNode -> providerExpressionForFactoryImplementation(
                        binding
                    )
                    is InstanceBindingNode -> providerExpressionForInstance(binding)
                    is MapBindingNode -> providerExpressionForMap(binding)
                    is NullBindingNode -> providerExpressionForNull(binding)
                    is ProviderBindingNode -> providerExpressionForProvider(binding)
                    is ProvisionBindingNode -> providerExpressionForProvision(binding)
                    is SetBindingNode -> providerExpressionForSet(binding)
                }
            }
        }

        bindingExpressions[request] = expression
        return expression
    }

    private fun instanceExpressionForAssistedProvision(binding: AssistedProvisionBindingNode): FactoryExpression {
        return {
            val (assistedParameters, nonAssistedParameters) = binding.parameters
                .partition { it.assisted }

            val dependencyExpressions = binding.dependencies
                .map { getBindingExpression(it) }

            InjektDeclarationIrBuilder(pluginContext, factory.factoryFunction.symbol)
                .irLambda(
                    pluginContext.tmpFunction(
                        assistedParameters
                            .size
                    ).typeWith(
                        assistedParameters
                            .map { it.type } +
                                binding.key.type.typeArguments.last().typeOrFail
                    )
                ) { lambda ->
                    +irReturn(
                        binding.createExpression(
                            this,
                            binding.parameters
                                .associateWith { parameter ->
                                    val expr: () -> IrExpression? = if (parameter.assisted) {
                                        {
                                            irGet(
                                                lambda.valueParameters[assistedParameters.indexOf(
                                                    parameter
                                                )]
                                            )
                                        }
                                    } else {
                                        {
                                            dependencyExpressions[nonAssistedParameters.indexOf(
                                                parameter
                                            )](this)
                                        }
                                    }
                                    expr
                                }
                        )
                    )
                }
        }
    }

    private fun instanceExpressionForChildFactory(binding: ChildFactoryBindingNode): FactoryExpression {
        return { binding.childFactoryExpression(this) }
    }

    private fun instanceExpressionForDelegate(binding: DelegateBindingNode): FactoryExpression =
        getBindingExpression(binding.dependencies.single())

    private fun instanceExpressionForDependency(binding: DependencyBindingNode): FactoryExpression {
        return {
            val dependencyExpression = getRequirementExpression(binding.requirementNode)
            irCall(binding.function).apply {
                dispatchReceiver = dependencyExpression()
            }
        }
    }

    private fun instanceExpressionForFactoryImplementation(
        binding: FactoryImplementationBindingNode
    ): FactoryExpression {
        return invokeProviderInstanceExpression(binding)
    }

    private fun instanceExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return getRequirementExpression(binding.requirementNode)
    }

    private fun instanceExpressionForMap(binding: MapBindingNode): FactoryExpression {
        return {
            val entryExpressions = binding.entries
                .map { (key, entryValue) ->
                    val entryValueExpression = getBindingExpression(
                        if (binding.valueKey.type.isNoArgProvider()) entryValue.copy(requestType = RequestType.Provider)
                        else entryValue
                    )
                    val pairExpression: FactoryExpression = pairExpression@{
                        irCall(pair.constructors.single()).apply {
                            putTypeArgument(0, binding.keyKey.type)
                            putTypeArgument(1, binding.valueKey.type)

                            putValueArgument(
                                0,
                                with(key) { asExpression() }
                            )
                            putValueArgument(
                                1,
                                entryValueExpression()
                            )
                        }
                    }
                    pairExpression
                }

            when (entryExpressions.size) {
                0 -> {
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.collections.emptyMap"))
                            .single()
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)
                    }
                }
                1 -> {
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.collections.mapOf"))
                            .single {
                                it.owner.valueParameters.singleOrNull()?.isVararg == false
                            }
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)
                        putValueArgument(
                            0,
                            entryExpressions.single()(),
                        )
                    }
                }
                else -> {
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.collections.mapOf"))
                            .single {
                                it.owner.valueParameters.singleOrNull()?.isVararg == true
                            }
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)
                        putValueArgument(
                            0,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                this@FactoryExpressions.pluginContext.irBuiltIns.arrayClass
                                    .typeWith(
                                        pair.typeWith(
                                            binding.keyKey.type,
                                            binding.valueKey.type
                                        )
                                    ),
                                pair.typeWith(
                                    binding.keyKey.type,
                                    binding.valueKey.type
                                ),
                                entryExpressions.map { it()!! }
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun instanceExpressionForNull(binding: NullBindingNode): FactoryExpression {
        return { irNull() }
    }

    private fun instanceExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(
                key = binding.key.type.typeArguments.single().typeOrFail.asKey(),
                requestingKey = binding.key,
                requestOrigin = binding.origin,
                requestType = RequestType.Provider
            )
        )
    }

    private fun instanceExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        return if (binding.scoped) {
            invokeProviderInstanceExpression(binding)
        } else {
            val expression: FactoryExpression = bindingExpression@{
                val dependencies = binding.dependencies
                    .map { getBindingExpression(it) }

                binding.createExpression(
                    this,
                    binding.parameters
                        .mapIndexed { index, parameter ->
                            index to parameter
                        }
                        .associateWith { (index, parameter) ->
                            { dependencies[index]() }
                        }
                        .mapKeys { it.key.second }
                )
            }
            expression
        }
    }

    private fun instanceExpressionForSet(binding: SetBindingNode): FactoryExpression {
        return bindingExpression@{
            val elementExpressions = binding.dependencies
                .map { element ->
                    getBindingExpression(
                        if (binding.elementKey.type.isNoArgProvider()) element.copy(requestType = RequestType.Provider)
                        else element
                    )
                }

            when (elementExpressions.size) {
                0 -> {
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.collections.emptySet"))
                            .single()
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                    }
                }
                1 -> {
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.collections.setOf"))
                            .single {
                                it.owner.valueParameters.singleOrNull()?.isVararg == false
                            }
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                        putValueArgument(
                            0,
                            elementExpressions.single()()
                        )
                    }
                }
                else -> {
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.collections.setOf"))
                            .single {
                                it.owner.valueParameters.singleOrNull()?.isVararg == true
                            }
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                        putValueArgument(
                            0,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                pluginContext.irBuiltIns.arrayClass
                                    .typeWith(binding.elementKey.type),
                                binding.elementKey.type,
                                elementExpressions.map { it()!! }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun providerExpressionForAssistedProvision(binding: AssistedProvisionBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) {
            singleInstanceFactory(
                getBindingExpression(
                    BindingRequest(
                        key = binding.key,
                        requestingKey = binding.key,
                        requestOrigin = binding.origin,
                        requestType = RequestType.Instance
                    )
                )()!!
            )
        }
    }

    private fun providerExpressionForChildFactory(binding: ChildFactoryBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) {
            singleInstanceFactory(binding.childFactoryExpression(this)!!)
        }
    }

    private fun providerExpressionForDelegate(binding: DelegateBindingNode): FactoryExpression =
        getBindingExpression(binding.dependencies.single().copy(requestType = RequestType.Provider))

    private fun providerExpressionForDependency(binding: DependencyBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) providerFieldExpression@{
            val dependencyExpression = getRequirementExpression(binding.requirementNode)
            InjektDeclarationIrBuilder(pluginContext, factory.factoryFunction.symbol)
                .irLambda(
                    pluginContext.tmpFunction(0)
                        .typeWith(binding.key.type)
                ) {
                    +irReturn(
                        irCall(binding.function).apply {
                            dispatchReceiver = dependencyExpression()
                        }
                    )
                }
        }
    }

    private fun providerExpressionForFactoryImplementation(
        binding: FactoryImplementationBindingNode
    ): FactoryExpression {
        factory as FactoryImpl

        return cachedFactoryExpression(binding.key) {
            irCall(symbols.lateinitFactory.constructors.single()).apply {
                putTypeArgument(0, factory.clazz.superTypes.single())
            }
        }.also { factory.factoryLateinitProvider = it }
    }

    private fun providerExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) {
            singleInstanceFactory(binding.requirementNode.accessor(this)!!)
        }
    }

    private fun providerExpressionForMap(binding: MapBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) providerFieldExpression@{
            val entryExpressions = binding.entries
                .map { (key, entryValue) ->
                    val entryValueExpression = getBindingExpression(
                        BindingRequest(
                            key = entryValue.key,
                            requestingKey = binding.key,
                            requestOrigin = binding.origin,
                            requestType = RequestType.Provider
                        )
                    )
                    val pairExpression: FactoryExpression = pairExpression@{
                        irCall(pair.constructors.single()).apply {
                            putTypeArgument(0, binding.keyKey.type)
                            putTypeArgument(1, binding.valueKey.type)

                            putValueArgument(
                                0,
                                with(key) { asExpression() }
                            )
                            putValueArgument(
                                1,
                                entryValueExpression()
                            )
                        }
                    }
                    pairExpression
                }

            val mapFactoryCompanion = if (binding.valueKey.type.isNoArgProvider()) {
                symbols.mapOfProviderFactory.owner.companionObject() as IrClass
            } else symbols.mapOfValueFactory.owner.companionObject() as IrClass

            if (entryExpressions.isEmpty()) {
                irCall(
                    mapFactoryCompanion.functions
                        .single { it.name.asString() == "empty" }
                ).apply {
                    dispatchReceiver = irGetObject(mapFactoryCompanion.symbol)
                    putTypeArgument(0, binding.keyKey.type)
                    putTypeArgument(1, binding.valueKey.type)
                }
            } else {
                when (entryExpressions.size) {
                    1 -> {
                        val create = mapFactoryCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        !it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(mapFactoryCompanion.symbol)
                            putTypeArgument(0, binding.keyKey.type)
                            putTypeArgument(1, binding.valueKey.type)
                            putValueArgument(
                                0,
                                entryExpressions.single()()
                            )
                        }
                    }
                    else -> {
                        val create = mapFactoryCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(mapFactoryCompanion.symbol)
                            putTypeArgument(0, binding.keyKey.type)
                            putTypeArgument(1, binding.valueKey.type)
                            putValueArgument(
                                0,
                                IrVarargImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    this@FactoryExpressions.pluginContext.irBuiltIns.arrayClass
                                        .typeWith(
                                            pair.typeWith(
                                                binding.keyKey.type,
                                                binding.valueKey.type
                                            )
                                        ),
                                    pair.typeWith(
                                        binding.keyKey.type,
                                        binding.valueKey.type
                                    ),
                                    entryExpressions.map { it()!! }
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun providerExpressionForNull(binding: NullBindingNode): FactoryExpression {
        return { singleInstanceFactory(irNull()) }
    }

    private fun providerExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(
                key = binding.dependencies.single().key,
                requestingKey = binding.key,
                requestOrigin = binding.origin,
                requestType = RequestType.Provider
            )
        )
    }

    private fun providerExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) providerFieldExpression@{
            val dependencyExpressions = binding.dependencies
                .map { getBindingExpression(it) }

            val newProvider =
                InjektDeclarationIrBuilder(pluginContext, factory.factoryFunction.symbol)
                    .irLambda(
                        pluginContext.tmpFunction(0)
                            .typeWith(binding.key.type)
                    ) {
                        +irReturn(
                            binding.createExpression(
                                this,
                                binding.parameters
                                    .mapIndexed { index, parameter ->
                                        index to parameter
                                    }
                                    .associateWith { (index, parameter) ->
                                        { dependencyExpressions[index]() }
                                    }
                                    .mapKeys { it.key.second }
                            )
                        )
                    }

            if (binding.scoped) {
                doubleCheck(newProvider)
            } else {
                newProvider
            }
        }
    }

    private fun providerExpressionForSet(binding: SetBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) providerFieldExpression@{
            val elementExpressions = binding.dependencies
                .map {
                    getBindingExpression(
                        it.copy(requestType = RequestType.Provider)
                    )
                }

            val setFactoryCompanion = if (binding.elementKey.type.isNoArgProvider()) {
                symbols.setOfProviderFactory.owner.companionObject() as IrClass
            } else symbols.setOfValueFactory.owner.companionObject() as IrClass

            if (elementExpressions.isEmpty()) {
                irCall(
                    setFactoryCompanion.functions
                        .single { it.name.asString() == "empty" }
                ).apply {
                    dispatchReceiver = irGetObject(setFactoryCompanion.symbol)
                    putTypeArgument(0, binding.elementKey.type)
                }
            } else {
                when (elementExpressions.size) {
                    1 -> {
                        val create = setFactoryCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        !it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(setFactoryCompanion.symbol)
                            putTypeArgument(0, binding.elementKey.type)
                            putValueArgument(
                                0,
                                elementExpressions.single()()
                            )
                        }
                    }
                    else -> {
                        val create = setFactoryCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(setFactoryCompanion.symbol)
                            putTypeArgument(0, binding.elementKey.type)
                            putValueArgument(
                                0,
                                IrVarargImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    this@FactoryExpressions.pluginContext.irBuiltIns.arrayClass
                                        .typeWith(
                                            pluginContext.tmpFunction(0)
                                                .typeWith(binding.key.type)
                                        ),
                                    binding.elementKey.type,
                                    elementExpressions.map { it()!! }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun cachedFactoryExpression(
        key: Key,
        factoryInitializer: IrBuilderWithScope.() -> IrExpression
    ): FactoryExpression {
        return members.cachedValue(
            pluginContext.tmpFunction(0)
                .typeWith(key.type)
                .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                .asKey(),
            factoryInitializer
        )
    }

    private fun IrBuilderWithScope.singleInstanceFactory(instance: IrExpression): IrExpression {
        val instanceProviderCompanion = symbols.singleInstanceFactory.owner
            .companionObject() as IrClass
        return irCall(
            instanceProviderCompanion
                .declarations
                .filterIsInstance<IrFunction>()
                .single { it.name.asString() == "create" }
        ).apply {
            dispatchReceiver = irGetObject(instanceProviderCompanion.symbol)
            putValueArgument(0, instance)
        }
    }

    private fun IrBuilderWithScope.doubleCheck(provider: IrExpression): IrExpression {
        return irCall(
            symbols.doubleCheck
                .constructors
                .single()
        ).apply { putValueArgument(0, provider) }
    }

    private fun FactoryExpression.wrapInFunction(key: Key): FactoryExpression {
        val factoryExpression = this
        val function = members.getGetFunction(key) function@{
            factoryExpression()!!
        }
        return bindingExpression@{ irCall(function) }
    }

    private fun invokeProviderInstanceExpression(binding: BindingNode): FactoryExpression {
        val providerExpression = getBindingExpression(
            BindingRequest(
                key = binding.key,
                requestingKey = binding.key,
                requestOrigin = binding.origin,
                requestType = RequestType.Provider
            )
        )
        return bindingExpression@{
            irCall(
                pluginContext.tmpFunction(0)
                    .functions
                    .single { it.owner.name.asString() == "invoke" }
            ).apply {
                dispatchReceiver = providerExpression()
            }
        }
    }

}

typealias FactoryExpression = IrBuilderWithScope.() -> IrExpression?
