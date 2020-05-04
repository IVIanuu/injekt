package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.typeArguments
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

typealias FactoryExpression = IrBuilderWithScope.(() -> IrExpression) -> IrExpression

class FactoryExpressions(
    private val context: IrPluginContext,
    private val symbols: InjektSymbols,
    private val members: FactoryMembers
) {

    // todo remove this circular dep
    lateinit var graph: Graph

    private val bindingExpressions = mutableMapOf<BindingRequest, FactoryExpression>()
    private val chain = mutableSetOf<BindingRequest>()

    private val requirementExpressions = mutableMapOf<RequirementNode, FactoryExpression>()

    private val collectionsScope = symbols.getPackage(FqName("kotlin.collections"))
    private val kotlinScope = symbols.getPackage(FqName("kotlin"))

    private val pair = context.symbolTable.referenceClass(
        kotlinScope.memberScope
            .getContributedClassifier(
                Name.identifier("Pair"),
                NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    ).ensureBound(context.irProviders)

    fun getRequirementExpression(node: RequirementNode): FactoryExpression {
        requirementExpressions[node]?.let { return it }
        val field = members.getOrCreateField(node.key, node.prefix) fieldInit@{ parent ->
            node.initializerAccessor(this, parent)
        }
        val expression: FactoryExpression = { irGetField(it(), field.field) }
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
                    is AssistedProvisionBindingNode -> instanceExpressionForAssistedProvision(
                        binding
                    )
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

        chain -= request

        bindingExpressions[request] = expression
        return expression
    }

    private fun instanceExpressionForAssistedProvision(binding: AssistedProvisionBindingNode): FactoryExpression {
        return invokeProviderInstanceExpression(binding)
    }

    private fun instanceExpressionForDelegate(binding: DelegateBindingNode): FactoryExpression =
        getBindingExpression(BindingRequest(binding.originalKey, RequestType.Instance))

    private fun instanceExpressionForDependency(binding: DependencyBindingNode): FactoryExpression {
        val dependencyExpression = getRequirementExpression(binding.requirementNode)
        val expression: FactoryExpression = bindingExpression@{ parent ->
            val provider = binding.provider

            val providerCompanion = provider.companionObject()!! as IrClass

            val createFunction = providerCompanion.functions
                .single { it.name.asString() == "create" }

            irCall(createFunction).apply {
                dispatchReceiver = irGetObject(providerCompanion.symbol)
                putValueArgument(
                    0,
                    dependencyExpression(this@bindingExpression, parent)
                )
            }
        }

        return expression.wrapInFunction(binding.key)
    }

    private fun instanceExpressionForFactoryImplementation(
        binding: FactoryImplementationBindingNode
    ): FactoryExpression {
        return { it() }
    }

    private fun instanceExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return getRequirementExpression(binding.requirementNode)
    }

    private fun instanceExpressionForLazy(binding: LazyBindingNode): FactoryExpression {
        return {
            doubleCheck(
                getBindingExpression(
                    BindingRequest(
                        Key(binding.key.type.typeArguments.single()),
                        RequestType.Provider
                    )
                )
                    (this, it)
            )
        }
    }

    private fun instanceExpressionForMap(binding: MapBindingNode): FactoryExpression {
        val entryExpressions = binding.entries
            .map { (key, entryValue) ->
                val entryValueExpression = getBindingExpression(entryValue.asBindingRequest())
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

        val expression: FactoryExpression = bindingExpression@{ parent ->
            when (entryExpressions.size) {
                0 -> {
                    irCall(
                        this@FactoryExpressions.context.symbolTable.referenceFunction(
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
                        this@FactoryExpressions.context.symbolTable.referenceFunction(
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
                            entryExpressions.single()(this@bindingExpression, parent),
                        )
                    }
                }
                else -> {
                    irCall(
                        this@FactoryExpressions.context.symbolTable.referenceFunction(
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
                                context.irBuiltIns.arrayClass
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
                                    it(this@bindingExpression, parent)
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
            BindingRequest(Key(binding.key.type.typeArguments.single()), RequestType.Provider)
        )
    }

    private fun instanceExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val expression = if (binding.scoped) {
            invokeProviderInstanceExpression(binding)
        } else {
            val provider = binding.provider

            val dependencies = binding.dependencies
                .map {
                    getBindingExpression(it.asBindingRequest())
                }

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
                    val providerCompanion = provider.companionObject()!! as IrClass

                    irCall(createFunction).apply {
                        dispatchReceiver = irGetObject(providerCompanion.symbol)

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
                                        parent
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
            .map { getBindingExpression(it.asBindingRequest()) }

        val expression: FactoryExpression = bindingExpression@{ parent ->
            when (elementExpressions.size) {
                0 -> {
                    irCall(
                        this@FactoryExpressions.context.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findSingleFunction(Name.identifier("emptySet"))
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                    }
                }
                1 -> {
                    irCall(
                        this@FactoryExpressions.context.symbolTable.referenceFunction(
                            collectionsScope.memberScope.findFirstFunction("setOf") {
                                it.valueParameters.singleOrNull()?.isVararg == false
                            }
                        ),
                        binding.key.type
                    ).apply {
                        putTypeArgument(0, binding.elementKey.type)
                        putValueArgument(
                            0,
                            elementExpressions.single()(this@bindingExpression, parent)
                        )
                    }
                }
                else -> {
                    irCall(
                        this@FactoryExpressions.context.symbolTable.referenceFunction(
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
                                context.irBuiltIns.arrayClass
                                    .typeWith(binding.elementKey.type),
                                binding.elementKey.type,
                                elementExpressions.map {
                                    it(this@bindingExpression, parent)
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

        val dependencyKeys = binding.dependencies
            .map { it.key }

        val dependencyExpressions = binding.dependencies
            .map { getBindingExpression(BindingRequest(it.key, it.requestType)) }

        return providerFieldExpression(binding.key) { parent ->
            if (dependencyKeys.any {
                    it in members.fields && it !in members.initializedFields
                }) return@providerFieldExpression null

            instanceProvider(
                irCall(constructor).apply {
                    dependencyExpressions.forEachIndexed { index, dependency ->
                        putValueArgument(index, dependency(this@providerFieldExpression, parent))
                    }
                }
            )
        }
    }

    private fun providerExpressionForDelegate(binding: DelegateBindingNode): FactoryExpression =
        getBindingExpression(BindingRequest(binding.originalKey, RequestType.Provider))

    private fun providerExpressionForDependency(binding: DependencyBindingNode): FactoryExpression {
        return providerFieldExpression(binding.key) providerFieldExpression@{
            val provider = binding.provider
            irCall(provider.constructors.single()).apply {
                putValueArgument(
                    0, binding.requirementNode
                        .initializerAccessor(this@providerFieldExpression, it)
                )
            }
        }
    }

    private fun providerExpressionForFactoryImplementation(
        binding: FactoryImplementationBindingNode
    ): FactoryExpression {
        return providerFieldExpression(binding.key) { instanceProvider(it()) }
    }

    private fun providerExpressionForInstance(binding: InstanceBindingNode): FactoryExpression {
        return providerFieldExpression(binding.key) {
            instanceProvider(
                binding.requirementNode
                    .initializerAccessor(this, it)
            )
        }
    }

    private fun providerExpressionForLazy(binding: LazyBindingNode): FactoryExpression {
        val dependencyExpression = getBindingExpression(
            BindingRequest(Key(binding.key.type.typeArguments.single()), RequestType.Provider)
        )
        return providerFieldExpression(binding.key) {
            irCall(
                symbols.getProviderOfLazy(0)
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
                    BindingRequest(entryValue.key, RequestType.Provider)
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

        return providerFieldExpression(binding.key) providerFieldExpression@{ parent ->
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
                                entryExpressions.single()(this@providerFieldExpression, parent)
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
                                    context.irBuiltIns.arrayClass
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
                                        it(this@providerFieldExpression, parent)
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

        val dependencyKeys = binding.dependencies
            .map { it.key }

        val dependencyExpressions = binding.dependencies
            .map { getBindingExpression(BindingRequest(it.key, it.requestType)) }

        return providerFieldExpression(binding.key) { parent ->
            if (dependencyKeys.any {
                    it in members.fields && it !in members.initializedFields
                }) return@providerFieldExpression null

            instanceProvider(
                irCall(constructor).apply {
                    dependencyExpressions.forEachIndexed { index, dependency ->
                        putValueArgument(index, dependency(this@providerFieldExpression, parent))
                    }
                }
            )
        }
    }

    private fun providerExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(Key(binding.key.type.typeArguments.single()), RequestType.Provider)
        )
    }

    private fun providerExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val dependencyKeys = binding.dependencies
            .map { Key(symbols.getFunction(0).typeWith(it.key.type)) }

        val provider = binding.provider

        val moduleRequired =
            provider.constructors.single().valueParameters.firstOrNull()
                ?.name?.asString() == "module"

        val dependencies = binding.dependencies
            .map { getBindingExpression(BindingRequest(it.key, RequestType.Provider)) }

        return if (!moduleRequired && dependencies.isEmpty() && !binding.scoped) {
            { irGetObject(provider.symbol) }
        } else {
            providerFieldExpression(binding.key) providerFieldExpression@{ parent ->
                if (dependencyKeys.any {
                        it in members.fields && it !in members.initializedFields
                    }) return@providerFieldExpression null

                val newProvider = if (!moduleRequired && dependencies.isEmpty()) {
                    irGetObject(provider.symbol)
                } else {
                    irCall(provider.constructors.single()).apply {
                        if (moduleRequired) {
                            putValueArgument(
                                0,
                                binding.module!!.initializerAccessor(
                                    this@providerFieldExpression,
                                    parent
                                )
                            )
                        }

                        dependencies.forEachIndexed { index, dependency ->
                            val realIndex = index + if (moduleRequired) 1 else 0
                            putValueArgument(
                                realIndex,
                                dependency(this@providerFieldExpression, parent)
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
            .map { getBindingExpression(BindingRequest(it.key, RequestType.Provider)) }

        return providerFieldExpression(binding.key) providerFieldExpression@{ parent ->
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
                                elementExpressions.single()(this@providerFieldExpression, parent)
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
                                    context.irBuiltIns.arrayClass
                                        .typeWith(
                                            symbols.getFunction(0)
                                                .typeWith(binding.key.type)
                                        ),
                                    binding.elementKey.type,
                                    elementExpressions.map {
                                        it(this@providerFieldExpression, parent)
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
        providerInitializer: IrBuilderWithScope.(() -> IrExpression) -> IrExpression?
    ): FactoryExpression {
        val field = members.getOrCreateField(
            Key(symbols.getFunction(0).typeWith(key.type)),
            "provider",
            providerInitializer
        )
        return { irGetField(it(), field.field) }
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
            symbols.getDoubleCheck(0)
                .constructors
                .single()
        ).apply { putValueArgument(0, provider) }
    }

    private fun FactoryExpression.wrapInFunction(key: Key): FactoryExpression {
        val factoryExpression = this
        val function = members.getGetFunction(key) function@{ function ->
            factoryExpression(this) {
                irGet(function.dispatchReceiverParameter!!)
            }
        }
        return bindingExpression@{
            irCall(function).apply {
                dispatchReceiver = it()
            }
        }
    }

    private fun invokeProviderInstanceExpression(binding: BindingNode): FactoryExpression {
        val providerExpression = getBindingExpression(
            BindingRequest(binding.key, RequestType.Provider)
        )
        return bindingExpression@{ parent ->
            irCall(
                symbols.getFunction(0)
                    .functions
                    .single { it.owner.name.asString() == "invoke" }
            ).apply {
                dispatchReceiver = providerExpression(this@bindingExpression, parent)
            }
        }
    }
}

