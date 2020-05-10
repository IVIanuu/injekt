package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class FactoryExpressions(
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val members: FactoryMembers,
    private val parent: FactoryExpressions?,
    private val factoryProduct: AbstractFactoryProduct
) {

    // todo remove this circular dep
    lateinit var graph: Graph

    private val bindingExpressions = mutableMapOf<BindingRequest, FactoryExpression>()

    private val requirementExpressions = mutableMapOf<RequirementNode, FactoryExpression>()

    private val collectionsScope = symbols.getPackage(FqName("kotlin.collections"))
    private val kotlinScope = symbols.getPackage(FqName("kotlin"))

    private val pair = pluginContext.symbolTable.referenceClass(
        kotlinScope.memberScope
            .getContributedClassifier(
                Name.identifier("Pair"),
                NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    ).ensureBound(pluginContext.irProviders)

    fun getRequirementExpression(node: RequirementNode): FactoryExpression {
        requirementExpressions[node]?.let { return it }
        val expression = members.cachedValue(
            node.key,
            node.prefix
        ) {
            node.initializerAccessor(
                this,
                if (factoryProduct is FactoryImplementation) {
                    it.factoryAccessors[factoryProduct]!!
                } else {
                    { error("Unsupported") }
                }
            )
        }
        requirementExpressions[node] = expression
        return expression
    }

    fun getBindingExpression(request: BindingRequest): FactoryExpression {
        bindingExpressions[request]?.let { return it }

        val binding = graph.getBinding(request)

        if (binding.owner != factoryProduct) {
            return parent?.getBindingExpression(request)!!
        }

        val expression = when (request.requestType) {
            RequestType.Instance -> {
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
                    is LazyBindingNode -> instanceExpressionForLazy(binding)
                    is MapBindingNode -> instanceExpressionForMap(binding)
                    is MembersInjectorBindingNode -> instanceExpressionForMembersInjector(binding)
                    is ProviderBindingNode -> instanceExpressionForProvider(binding)
                    is ProvisionBindingNode -> instanceExpressionForProvision(binding)
                    is SetBindingNode -> instanceExpressionForSet(binding)
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
                    is LazyBindingNode -> providerExpressionForLazy(binding)
                    is MapBindingNode -> providerExpressionForMap(binding)
                    is MembersInjectorBindingNode -> providerExpressionForMembersInjector(binding)
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
        return invokeProviderInstanceExpression(binding)
    }

    private fun instanceExpressionForChildFactory(binding: ChildFactoryBindingNode): FactoryExpression {
        return invokeProviderInstanceExpression(binding)
    }

    private fun instanceExpressionForDelegate(binding: DelegateBindingNode): FactoryExpression =
        getBindingExpression(binding.dependencies.single())

    private fun instanceExpressionForDependency(binding: DependencyBindingNode): FactoryExpression {
        val dependencyExpression = getRequirementExpression(binding.requirementNode)
        val expression: FactoryExpression = bindingExpression@{ context ->
            val provider = binding.provider

            val providerCompanion = provider.companionObject()!! as IrClass

            val createFunction = providerCompanion.functions
                .single { it.name.asString() == "create" }

            irCall(createFunction).apply {
                dispatchReceiver = irGetObject(providerCompanion.symbol)
                putValueArgument(
                    0,
                    dependencyExpression(this@bindingExpression, context)
                )
            }
        }

        return expression.wrapInFunction(binding.key)
    }

    private fun instanceExpressionForFactoryImplementation(
        binding: FactoryImplementationBindingNode
    ): FactoryExpression {
        return { it[factoryProduct] }
    }

    private fun instanceExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return getRequirementExpression(binding.requirementNode)
    }

    private fun instanceExpressionForLazy(binding: LazyBindingNode): FactoryExpression {
        return {
            doubleCheck(
                getBindingExpression(
                    binding.dependencies.single().copy(
                        requestType = RequestType.Provider
                    )
                )(this, it)
            )
        }
    }

    private fun instanceExpressionForMap(binding: MapBindingNode): FactoryExpression {
        val entryExpressions = binding.entries
            .map { (key, entryValue) ->
                val entryValueExpression = getBindingExpression(entryValue)
                val pairExpression: FactoryExpression = pairExpression@{
                    irCall(
                        pair.constructors.single(),
                        pair.typeWith(binding.keyKey.type, binding.valueKey.type)
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)

                        putValueArgument(
                            0,
                            with(key) { asExpression() }
                        )
                        putValueArgument(
                            1,
                            entryValueExpression(this@pairExpression, it)
                        )
                    }
                }
                pairExpression
            }

        val expression: FactoryExpression = bindingExpression@{ context ->
            when (entryExpressions.size) {
                0 -> {
                    irCall(
                        this@FactoryExpressions.pluginContext.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findSingleFunction(Name.identifier("emptyMap"))
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)
                    }
                }
                1 -> {
                    irCall(
                        this@FactoryExpressions.pluginContext.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findFirstFunction("mapOf") {
                                it.valueParameters.singleOrNull()?.isVararg == false
                            }
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)
                        putValueArgument(
                            0,
                            entryExpressions.single()(this@bindingExpression, context),
                        )
                    }
                }
                else -> {
                    irCall(
                        this@FactoryExpressions.pluginContext.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findFirstFunction("mapOf") {
                                it.valueParameters.singleOrNull()?.isVararg == true
                            }
                        ),
                        binding.key.type
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
                                entryExpressions.map {
                                    it(this@bindingExpression, context)
                                }
                            ),
                        )
                    }
                }
            }
        }

        return expression.wrapInFunction(binding.key)
    }

    private fun instanceExpressionForMembersInjector(binding: MembersInjectorBindingNode): FactoryExpression {
        return invokeProviderInstanceExpression(binding)
    }

    private fun instanceExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(
                binding.key.type.typeArguments.single().asKey(pluginContext),
                binding.origin,
                RequestType.Provider
            )
        )
    }

    private fun instanceExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val expression = if (binding.scoped) {
            invokeProviderInstanceExpression(binding)
        } else {
            val provider = binding.provider

            val dependencies = binding.dependencies
                .map { getBindingExpression(it) }

            val moduleRequired =
                provider.kind != ClassKind.OBJECT && provider.constructors
                    .single().valueParameters.firstOrNull()?.name?.asString() == "module"

            val moduleExpression = if (moduleRequired) getRequirementExpression(binding.module!!)
            else null

            val expression: FactoryExpression = bindingExpression@{ context ->
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
                    val providerCompanion = provider.companionObject()!! as IrClass

                    irCall(createFunction).apply {
                        dispatchReceiver = irGetObject(providerCompanion.symbol)

                        if (moduleRequired) {
                            putValueArgument(
                                0,
                                moduleExpression!!(
                                    this@bindingExpression,
                                    context
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
                                        context
                                    )
                                )
                            }
                    }
                }
            }
            expression
        }

        return expression.wrapInFunction(binding.key)
    }

    private fun instanceExpressionForSet(binding: SetBindingNode): FactoryExpression {
        val elementExpressions = binding.dependencies
            .map { getBindingExpression(it) }

        val expression: FactoryExpression = bindingExpression@{ context ->
            when (elementExpressions.size) {
                0 -> {
                    irCall(
                        this@FactoryExpressions.pluginContext.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findSingleFunction(Name.identifier("emptySet"))
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                    }
                }
                1 -> {
                    irCall(
                        this@FactoryExpressions.pluginContext.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findFirstFunction("setOf") {
                                it.valueParameters.singleOrNull()?.isVararg == false
                            }
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                        putValueArgument(
                            0,
                            elementExpressions.single()(this@bindingExpression, context)
                        )
                    }
                }
                else -> {
                    irCall(
                        this@FactoryExpressions.pluginContext.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findFirstFunction("setOf") {
                                it.valueParameters.singleOrNull()?.isVararg == true
                            }
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                        putValueArgument(
                            0,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                this@FactoryExpressions.pluginContext.irBuiltIns.arrayClass
                                    .typeWith(binding.elementKey.type),
                                binding.elementKey.type,
                                elementExpressions.map {
                                    it(this@bindingExpression, context)
                                }
                            )
                        )
                    }
                }
            }
        }

        return expression.wrapInFunction(binding.key)
    }

    private fun providerExpressionForAssistedProvision(binding: AssistedProvisionBindingNode): FactoryExpression {
        val constructor = binding.provider.constructors.single()

        val dependencyExpressions = binding.dependencies
            .map { getBindingExpression(it) }

        return providerFieldExpression(binding.key) { context ->
            instanceProvider(
                irCall(constructor).apply {
                    dependencyExpressions.forEachIndexed { index, dependency ->
                        putValueArgument(index, dependency(this@providerFieldExpression, context))
                    }
                }
            )
        }
    }

    private fun providerExpressionForChildFactory(binding: ChildFactoryBindingNode): FactoryExpression {
        val constructor = binding.childFactory.constructors.single()

        val parentExpression = getBindingExpression(binding.dependencies.single())

        return providerFieldExpression(binding.key) { context ->
            instanceProvider(
                irCall(constructor).apply {
                    putValueArgument(0, parentExpression(this@providerFieldExpression, context))
                }
            )
        }
    }

    private fun providerExpressionForDelegate(binding: DelegateBindingNode): FactoryExpression =
        getBindingExpression(binding.dependencies.single().copy(requestType = RequestType.Provider))

    private fun providerExpressionForDependency(binding: DependencyBindingNode): FactoryExpression {
        return providerFieldExpression(binding.key) providerFieldExpression@{ context ->
            val provider = binding.provider
            irCall(provider.constructors.single()).apply {
                putValueArgument(
                    0, binding.requirementNode
                        .initializerAccessor(
                            this@providerFieldExpression,
                            context.factoryAccessors[factoryProduct]!!
                        )
                )
            }
        }
    }

    private fun providerExpressionForFactoryImplementation(
        binding: FactoryImplementationBindingNode
    ): FactoryExpression {
        return providerFieldExpression(binding.key) {
            instanceProvider(it[factoryProduct])
        }
    }

    private fun providerExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return providerFieldExpression(binding.key) {
            instanceProvider(
                binding.requirementNode
                    .initializerAccessor(this, it.factoryAccessors[factoryProduct]!!)
            )
        }
    }

    private fun providerExpressionForLazy(binding: LazyBindingNode): FactoryExpression {
        val dependencyExpression = getBindingExpression(
            BindingRequest(
                binding.key.type.typeArguments.single().asKey(pluginContext),
                binding.origin,
                RequestType.Provider
            )
        )
        return providerFieldExpression(binding.key) {
            irCall(
                symbols.providerOfLazy
                    .constructors
                    .single()
            ).apply {
                putValueArgument(0, dependencyExpression(this@providerFieldExpression, it))
            }
        }
    }

    private fun providerExpressionForMap(binding: MapBindingNode): FactoryExpression {
        val entryExpressions = binding.entries
            .map { (key, entryValue) ->
                val entryValueExpression = getBindingExpression(
                    BindingRequest(
                        entryValue.key,
                        binding.origin,
                        RequestType.Provider
                    )
                )
                val pairExpression: FactoryExpression = pairExpression@{
                    irCall(
                        pair.constructors.single(),
                        pair.typeWith(binding.keyKey.type, binding.valueKey.type)
                    ).apply {
                        putTypeArgument(0, binding.keyKey.type)
                        putTypeArgument(1, binding.valueKey.type)

                        putValueArgument(
                            0,
                            with(key) { asExpression() }
                        )
                        putValueArgument(
                            1,
                            entryValueExpression(this@pairExpression, it)
                        )
                    }
                }
                pairExpression
            }

        return providerFieldExpression(binding.key) providerFieldExpression@{ context ->
            val mapProviderCompanion = symbols.mapProvider.owner
                .companionObject() as IrClass

            if (entryExpressions.isEmpty()) {
                irCall(
                    mapProviderCompanion.functions
                        .single { it.name.asString() == "empty" }
                ).apply {
                    dispatchReceiver = irGetObject(mapProviderCompanion.symbol)
                    putTypeArgument(0, binding.keyKey.type)
                    putTypeArgument(1, binding.valueKey.type)
                }
            } else {
                when (entryExpressions.size) {
                    1 -> {
                        val create = mapProviderCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        !it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(mapProviderCompanion.symbol)
                            putTypeArgument(0, binding.keyKey.type)
                            putTypeArgument(1, binding.valueKey.type)
                            putValueArgument(
                                0,
                                entryExpressions.single()(this@providerFieldExpression, context)
                            )
                        }
                    }
                    else -> {
                        val create = mapProviderCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(mapProviderCompanion.symbol)
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
                                    entryExpressions.map {
                                        it(this@providerFieldExpression, context)
                                    }
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun providerExpressionForMembersInjector(binding: MembersInjectorBindingNode): FactoryExpression {
        val constructor = binding.membersInjector.constructors.single()

        val dependencyExpressions = binding.dependencies
            .map { getBindingExpression(it) }

        return providerFieldExpression(binding.key) { context ->
            instanceProvider(
                irCall(constructor).apply {
                    dependencyExpressions.forEachIndexed { index, dependency ->
                        putValueArgument(index, dependency(this@providerFieldExpression, context))
                    }
                }
            )
        }
    }

    private fun providerExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(
                binding.dependencies.single().key,
                binding.origin,
                RequestType.Provider
            )
        )
    }

    private fun providerExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val provider = binding.provider

        val moduleRequired =
            provider.constructors.singleOrNull()
                ?.valueParameters
                ?.firstOrNull()
                ?.name?.asString() == "module"

        val dependencies = binding.dependencies
            .map {
                getBindingExpression(
                    it.copy(requestType = RequestType.Provider)
                )
            }

        return if (!moduleRequired && dependencies.isEmpty() && !binding.scoped) {
            { irGetObject(provider.symbol) }
        } else {
            providerFieldExpression(binding.key) providerFieldExpression@{ context ->
                val newProvider = if (!moduleRequired && dependencies.isEmpty()) {
                    irGetObject(provider.symbol)
                } else {
                    irCall(provider.constructors.single()).apply {
                        if (moduleRequired) {
                            putValueArgument(
                                0,
                                binding.module!!.initializerAccessor(
                                    this@providerFieldExpression,
                                    context.factoryAccessors[factoryProduct]!!
                                )
                            )
                        }

                        dependencies.forEachIndexed { index, dependency ->
                            val realIndex = index + if (moduleRequired) 1 else 0
                            putValueArgument(
                                realIndex,
                                dependency(this@providerFieldExpression, context)
                            )
                        }
                    }
                }

                if (binding.scoped) {
                    doubleCheck(newProvider)
                } else {
                    newProvider
                }
            }
        }
    }

    private fun providerExpressionForSet(binding: SetBindingNode): FactoryExpression {
        val elementExpressions = binding.dependencies
            .map {
                getBindingExpression(
                    it.copy(requestType = RequestType.Provider)
                )
            }

        return providerFieldExpression(binding.key) providerFieldExpression@{ context ->
            val setProviderCompanion = symbols.setProvider.owner
                .companionObject() as IrClass

            if (elementExpressions.isEmpty()) {
                irCall(
                    setProviderCompanion.functions
                        .single { it.name.asString() == "empty" }
                ).apply {
                    dispatchReceiver = irGetObject(setProviderCompanion.symbol)
                    putTypeArgument(0, binding.elementKey.type)
                }
            } else {
                when (elementExpressions.size) {
                    1 -> {
                        val create = setProviderCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        !it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(setProviderCompanion.symbol)
                            putTypeArgument(0, binding.elementKey.type)
                            putValueArgument(
                                0,
                                elementExpressions.single()(this@providerFieldExpression, context)
                            )
                        }
                    }
                    else -> {
                        val create = setProviderCompanion.functions
                            .single {
                                it.name.asString() == "create" &&
                                        it.valueParameters.single().isVararg
                            }

                        irCall(create).apply {
                            dispatchReceiver = irGetObject(setProviderCompanion.symbol)
                            putTypeArgument(0, binding.elementKey.type)
                            putValueArgument(
                                0,
                                IrVarargImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    this@FactoryExpressions.pluginContext.irBuiltIns.arrayClass
                                        .typeWith(
                                            symbols.getFunction(0)
                                                .typeWith(binding.key.type)
                                        ),
                                    binding.elementKey.type,
                                    elementExpressions.map {
                                        it(this@providerFieldExpression, context)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun providerFieldExpression(
        key: Key,
        providerInitializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression
    ): FactoryExpression {
        return members.cachedValue(
            symbols.getFunction(
                    0
                )
                .typeWith(key.type)
                .withNoArgQualifiers(symbols, listOf(InjektFqNames.Provider))
                .asKey(pluginContext),
            "provider",
            providerInitializer
        )
    }

    private fun IrBuilderWithScope.instanceProvider(instance: IrExpression): IrExpression {
        val instanceProviderCompanion = symbols.instanceProvider.owner
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
        val function = members.getGetFunction(key) function@{ function ->
            factoryExpression(this, if (function.dispatchReceiverParameter != null) {
                FactoryExpressionContext(
                    factoryProduct
                ) { irGet(function.dispatchReceiverParameter!!) }
            } else EmptyFactoryExpressionContext)
        }
        return bindingExpression@{
            irCall(function).apply {
                if (function.dispatchReceiverParameter != null) {
                    dispatchReceiver = it[factoryProduct]
                }
            }
        }
    }

    private fun invokeProviderInstanceExpression(binding: BindingNode): FactoryExpression {
        val providerExpression = getBindingExpression(
            BindingRequest(
                binding.key,
                binding.origin,
                RequestType.Provider
            )
        )
        return bindingExpression@{ context ->
            irCall(
                symbols.getFunction(0)
                    .functions
                    .single { it.owner.name.asString() == "invoke" }
            ).apply {
                dispatchReceiver = providerExpression(this@bindingExpression, context)
            }
        }
    }
}

typealias FactoryExpression = IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression

data class FactoryExpressionContext(
    val factoryAccessors: Map<AbstractFactoryProduct, () -> IrExpression>
) {
    operator fun get(factoryProduct: AbstractFactoryProduct) =
        factoryAccessors.getValue(factoryProduct)()
}

val EmptyFactoryExpressionContext = FactoryExpressionContext(emptyMap())

fun IrBuilderWithScope.FactoryExpressionContext(
    factoryProduct: AbstractFactoryProduct,
    accessor: () -> IrExpression
): FactoryExpressionContext {
    val allImplementations = mutableListOf<AbstractFactoryProduct>()
    var current: FactoryImplementation? = factoryProduct as? FactoryImplementation
    if (current != null) {
        while (current != null) {
            allImplementations += current
            current = current.parent
        }
    } else {
        allImplementations += factoryProduct
    }
    val implementationWithAccessor =
        mutableMapOf<AbstractFactoryProduct, () -> IrExpression>()

    allImplementations.fold(accessor) { acc: () -> IrExpression, impl: AbstractFactoryProduct ->
        implementationWithAccessor[impl] = acc
        { irGetField(acc(), (impl as FactoryImplementation).parentField!!) }
    }

    return FactoryExpressionContext(implementationWithAccessor)
}
