package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.ensureBound
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
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
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
                    is DelegateBindingNode -> instanceExpressionForDelegate(binding)
                    is DependencyBindingNode -> instanceExpressionForDependency(binding)
                    is FactoryImplementationBindingNode -> instanceExpressionForFactoryImplementation(
                        binding
                    )
                    is InstanceBindingNode -> instanceExpressionForInstance(binding)
                    is LazyBindingNode -> instanceExpressionForLazy(binding)
                    is MapBindingNode -> instanceExpressionForMap(binding)
                    is ProviderBindingNode -> instanceExpressionForProvider(binding)
                    is ProvisionBindingNode -> instanceExpressionForProvision(binding)
                    is SetBindingNode -> instanceExpressionForSet(binding)
                }
            }
            RequestType.Provider -> {
                when (binding) {
                    is DelegateBindingNode -> providerExpressionForDelegate(binding)
                    is DependencyBindingNode -> providerExpressionForDependency(binding)
                    is FactoryImplementationBindingNode -> providerExpressionForFactoryImplementation(
                        binding
                    )
                    is InstanceBindingNode -> providerExpressionForInstance(binding)
                    is LazyBindingNode -> providerExpressionForLazy(binding)
                    is MapBindingNode -> providerExpressionForMap(binding)
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
                        binding.key.unwrapSingleArgKey(),
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

    private fun instanceExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(binding.key.unwrapSingleArgKey(), RequestType.Provider)
        )
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
            BindingRequest(binding.key.unwrapSingleArgKey(), RequestType.Provider)
        )
        return providerFieldExpression(binding.key) {
            irCall(
                symbols.providerOfLazy
                    .constructors
                    .single()
            ).apply { putValueArgument(0, dependencyExpression(this@providerFieldExpression, it)) }
        }
    }

    private fun providerExpressionForMap(binding: MapBindingNode): FactoryExpression {
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

        return providerFieldExpression(binding.key) providerFieldExpression@{ parent ->
            val mapProviderCompanion = symbols.mapProvider.owner
                .companionObject() as IrClass

            if (entryExpressions.isEmpty()) {
                irCall(
                    mapProviderCompanion.functions
                        .single { it.name.asString() == "empty" }
                ).apply {
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

    private fun providerExpressionForProvider(binding: ProviderBindingNode): FactoryExpression {
        return getBindingExpression(
            BindingRequest(binding.key.unwrapSingleArgKey(), RequestType.Provider)
        )
    }

    private fun providerExpressionForProvision(binding: ProvisionBindingNode): FactoryExpression {
        val dependencyKeys = binding.dependencies
            .map {
                Key(symbols.provider.typeWith(it.key.type).buildSimpleType {
                    annotations += it.key.type.annotations
                })
            }

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
                            putTypeArgument(0, binding.elementKey.type)
                            putValueArgument(
                                0,
                                IrVarargImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    context.irBuiltIns.arrayClass
                                        .typeWith(
                                            symbols.provider.typeWith(
                                                binding.key.type
                                            )
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
            Key(
                symbols.provider.typeWith(key.type).buildSimpleType {
                    annotations += key.type.annotations
                }
            ),
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
            symbols.doubleCheck
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
}

