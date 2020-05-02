package com.ivianuu.injekt.compiler.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Graph(
    val context: IrPluginContext,
    val symbols: InjektSymbols,
    val thisComponent: ComponentNode,
    thisComponentModule: ModuleNode?,
    declarationStore: InjektDeclarationStore
) {

    val scopes = mutableSetOf<FqName>()

    private val bindingResolvers = mutableListOf<BindingResolver>()
    private val annotatedClassBindingResolver =
        AnnotatedClassBindingResolver(this, context, declarationStore)
    val resolvedBindings = mutableMapOf<Key, Binding>()
    private val bindingRequestChain = mutableSetOf<Key>()

    init {
        if (thisComponentModule != null) addModule(thisComponentModule)
        bindingResolvers += LazyOrProviderBindingResolver(context, this)
    }

    fun requestBinding(key: Key): Binding {
        check(key !in bindingRequestChain) {
            "Circular dependency $key"
        }
        bindingRequestChain += key
        val binding = resolvedBindings.getOrPut(key) {
            val explicitBindings = bindingResolvers.flatMap { it(key) }
            if (explicitBindings.size > 1) {
                error("Multiple bindings found for $key")
            }

            val binding = explicitBindings.singleOrNull()
                ?: annotatedClassBindingResolver(key).singleOrNull()
                ?: error("No binding found for $key")

            if (binding.targetScope != null && binding.targetScope !in scopes) {
                error(
                    "Scope mismatch binding ${binding.key} " +
                            "with scope ${binding.targetScope} is not compatible with this component $scopes"
                )
            }

            binding
        }

        bindingRequestChain -= key
        return binding
    }

    private fun addScope(scope: FqName) {
        scopes += scope
    }

    private fun addModule(moduleNode: ModuleNode) {
        val module = moduleNode.module

        val descriptor = module.declarations.single {
            it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
        } as IrClass

        val functions = descriptor.functions

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstScope) }
            .forEach { addScope(it.returnType.classOrNull!!.descriptor.fqNameSafe) }

        // todo dependencies
        functions
            .filter { it.hasAnnotation(InjektFqNames.AstDependency) }
            .forEach { dependency ->

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
                        moduleNode.treeElement.child(field)
                    )
                )
            }

        bindingResolvers += ModuleBindingResolver(this, moduleNode, module, descriptor)
    }

    fun statefulBinding(
        key: Key,
        dependencies: List<Key>,
        targetScope: FqName? = null,
        providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
        getFunction: IrBuilderWithScope.() -> IrFunction,
        providerField: () -> IrField?
    ): Binding {
        return Binding(
            key = key,
            dependencies = dependencies,
            targetScope = targetScope,
            providerInstance = providerInstance,
            getFunction = getFunction,
            providerField = providerField
        )
    }

    fun statelessBinding(
        key: Key,
        dependencies: List<Key>,
        targetScope: FqName? = null,
        provider: IrClass,
        moduleIfRequired: ModuleNode? = null
    ): Binding {
        val createFunction = (if (provider.kind == ClassKind.OBJECT)
            provider else provider.declarations
            .single { it.nameForIrSerialization.asString() == "Companion" } as IrClass)
            .functions
            .single { it.name.asString() == "create" }

        return Binding(
            key = key,
            dependencies = dependencies,
            targetScope = targetScope,
            providerInstance = {
                if (provider.kind == ClassKind.OBJECT) {
                    irGetObject(provider.symbol)
                } else {
                    val constructor = provider.constructors.single()
                    irCall(constructor).apply {
                        if (moduleIfRequired != null) {
                            putValueArgument(
                                0,
                                moduleIfRequired.treeElement(
                                    this@Binding,
                                    it
                                )
                            )
                        }

                        constructor.valueParameters
                            .drop(if (moduleIfRequired != null) 1 else 0)
                            .forEach { valueParameter ->
                                val dependencyKey =
                                    dependencies[valueParameter.index - if (moduleIfRequired != null) 1 else 0]
                                val dependency = requestBinding(dependencyKey)
                                putValueArgument(
                                    valueParameter.index,
                                    dependency.providerInstance(
                                        this@Binding,
                                        it
                                    )
                                )
                            }
                    }
                }
            },
            getFunction = getFunction(key) { function ->
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

                        if (moduleIfRequired != null) {
                            putValueArgument(
                                0,
                                moduleIfRequired.treeElement(
                                    this@getFunction,
                                    irGet(function.dispatchReceiverParameter!!)
                                )
                            )
                        }

                        createFunction.valueParameters
                            .drop(if (moduleIfRequired != null) 1 else 0)
                            .forEach { valueParameter ->
                                val dependencyKey =
                                    dependencies[valueParameter.index - if (moduleIfRequired != null) 1 else 0]
                                val dependency = requestBinding(dependencyKey)
                                putValueArgument(
                                    valueParameter.index,
                                    irCall(dependency.getFunction(this@getFunction)).apply {
                                        dispatchReceiver = irGet(
                                            function.dispatchReceiverParameter!!
                                        )
                                    }
                                )
                            }
                    }
                }
            },
            { null }
        )
    }

    fun getFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrBuilderWithScope.() -> IrFunction {
        val function = thisComponent.component.addFunction {
            val currentGetFunctionIndex =
                context.irTrace[InjektWritableSlices.GET_FUNCTION_INDEX, thisComponent.component]
                    ?: 0
            this.name = Name.identifier("get_$currentGetFunctionIndex")
            context.irTrace.record(
                InjektWritableSlices.GET_FUNCTION_INDEX,
                thisComponent.component,
                currentGetFunctionIndex + 1
            )

            returnType = key.type
        }.apply {
            dispatchReceiverParameter = thisComponent.component.thisReceiver!!.copyTo(this)
        }
        return {
            if (function.body == null) {
                function.body = irExprBody(body(function))
            }
            function
        }
    }

    fun providerInvokeGetFunction(
        key: Key,
        treeElement: TreeElement
    ): IrBuilderWithScope.() -> IrFunction = getFunction(key) { function ->
        irCall(
            symbols.provider
                .functions
                .single { it.owner.name.asString() == "invoke" }
        ).apply {
            dispatchReceiver = treeElement(irGet(function.dispatchReceiverParameter!!))
        }
    }

    fun treeElementGetFunction(
        key: Key,
        treeElement: TreeElement
    ): IrBuilderWithScope.() -> IrFunction = getFunction(key) { function ->
        treeElement(irGet(function.dispatchReceiverParameter!!))
    }

    fun newProviderInstance(
        provider: IrClass,
        isScoped: Boolean,
        moduleIfRequired: ModuleNode?
    ): IrBuilderWithScope.(IrExpression) -> IrExpression = func@{
        val constructor = provider.constructors.single()
        val newProvider = irCall(constructor).apply {
            if (moduleIfRequired != null) {
                putValueArgument(
                    0,
                    moduleIfRequired.treeElement(
                        this@func,
                        it
                    )
                )
            }

            val dependencies = provider.constructors.single().valueParameters
                .filter { it.name.asString() != "module" }
                .map { Key(it.type) }

            constructor.valueParameters
                .drop(if (moduleIfRequired != null) 1 else 0)
                .forEach { valueParameter ->
                    val dependencyKey =
                        dependencies[valueParameter.index - if (moduleIfRequired != null) 1 else 0]
                    val dependency = requestBinding(dependencyKey)
                    putValueArgument(
                        valueParameter.index,
                        dependency.providerInstance(
                            this@func,
                            it
                        )
                    )
                }
        }

        if (isScoped) {
            irCall(
                symbols.doubleCheck
                    .constructors
                    .single()
            ).apply { putValueArgument(0, newProvider) }
        } else {
            newProvider
        }
    }

    fun allocateProviderField(instanceType: IrType): IrField {
        val currentProviderIndex =
            context.irTrace[InjektWritableSlices.PROVIDER_INDEX, thisComponent.component] ?: 0
        val field = thisComponent.component.addField(
            Name.identifier("provider_$currentProviderIndex"),
            symbols.provider.owner.typeWith(instanceType)
        )
        context.irTrace.record(
            InjektWritableSlices.PROVIDER_INDEX,
            thisComponent.component, currentProviderIndex + 1
        )

        return field
    }
}
