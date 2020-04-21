package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getStringList
import com.ivianuu.injekt.compiler.getTopLevelClass
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.substituteByName
import me.eugeniomarletti.kotlin.metadata.shadow.utils.addToStdlib.cast
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Graph(
    private val context: IrPluginContext,
    private val thisComponent: ComponentNode,
    thisComponentModule: ModuleNode,
    private val declarationStore: InjektDeclarationStore
) {

    private val parentScopes = mutableSetOf<FqName>()
    val thisScopes = mutableSetOf<FqName>()

    val thisBindings = mutableMapOf<Key, Binding>()
    val allBindings = mutableMapOf<Key, Binding>()

    private val allModules = mutableListOf<ModuleNode>()

    private val allParents = mutableListOf<ComponentNode>()
    val thisParents = mutableListOf<ComponentNode>()

    private val provider =
        context.moduleDescriptor.getTopLevelClass(InjektFqNames.Provider)
    private val providerFieldMetadata =
        context.moduleDescriptor.getTopLevelClass(InjektFqNames.ProviderFieldMetadata)
    private val singleProvider =
        context.moduleDescriptor.getTopLevelClass(InjektFqNames.SingleProvider)

    init {
        addModule(thisComponentModule)
        validate()
    }

    private fun addScope(scope: FqName, sourceComponent: ComponentNode) {
        check(scope !in thisScopes && scope !in parentScopes) {
            "Duplicated scope $scope"
        }

        if (sourceComponent == thisComponent) {
            thisScopes += scope
        } else {
            parentScopes += scope
        }
    }

    private fun addParent(parentNode: ComponentNode, fromModule: Boolean) {
        check(allParents.none { it.key == parentNode.key }) {
            "Duplicated parent $parentNode"
        }

        allParents += parentNode
        if (fromModule) thisParents += parentNode

        val metadata = parentNode.component.descriptor.annotations.single {
            it.fqName == InjektFqNames.ComponentMetadata
        }

        metadata.getStringList("scopes")
            .forEach { addScope(FqName(it), parentNode) }

        metadata.getStringList("parents")
            .forEach { parentKeyWithMaybePath ->
                val args = parentKeyWithMaybePath.split("=:=")
                val key = args[0]
                val path = args.getOrNull(1)
                val treeElement = if (path == null) null else {
                    val pathNames = path.split("/")
                        .filter { it.isNotEmpty() }
                    pathNames.fold(parentNode.treeElement!!) { acc: TreeElement, pathName: String ->
                        acc.childField(pathName)
                    }
                }

                val component = declarationStore.getComponent(key)

                addParent(
                    ComponentNode(
                        key = key,
                        component = component,
                        treeElement = treeElement
                    ),
                    false
                )
            }

        metadata.getStringList("bindings")
            .map { providerPathOrClass ->
                if (providerPathOrClass.startsWith("/")) {
                    val field = parentNode.component.declarations
                        .filterIsInstance<IrField>()
                        .single { it.name.asString() == providerPathOrClass.removePrefix("/") }

                    val provider = field.descriptor.annotations.single {
                            it.fqName == InjektFqNames.ProviderFieldMetadata
                        }.argumentValue("implementation")!!.value
                        .let { declarationStore.getProvider(FqName(it.toString())) }

                    val bindingMetadata = provider.descriptor.annotations.single {
                        it.fqName == InjektFqNames.BindingMetadata
                    }

                    val qualifiers = bindingMetadata.getStringList("qualifiers")
                        .map { FqName(it) }

                    val createFunction = (if (provider.kind == ClassKind.OBJECT)
                        provider else provider.companionObject() as IrClass)
                        .functions
                        .single { it.name.asString() == "create" }

                    val dependencies = createFunction.valueParameters
                        .map {
                            Key(
                                it.type,
                                it.descriptor.annotations.single {
                                        it.fqName == InjektFqNames.BindingMetadata
                                    }.getStringList("qualifiers")
                                    .map { FqName(it) }
                            )
                        }

                    val resultType = createFunction.returnType

                    StatefulBinding(
                        key = Key(resultType, qualifiers),
                        dependencies = dependencies,
                        provider = provider,
                        providerInstance = {
                            irGetField(
                                parentNode.treeElement!!.accessor(
                                    this,
                                    it
                                ),
                                field
                            )
                        },
                        getFunction = getFunction(resultType) { function ->
                            irCall(
                                this@Graph.context.symbolTable.referenceClass(this@Graph.provider)
                                    .ensureBound(this@Graph.context.irProviders)
                                    .owner
                                    .functions
                                    .single { it.name.asString() == "invoke" }
                                    .symbol,
                                resultType
                            ).apply {
                                dispatchReceiver = irGetField(
                                    parentNode.treeElement!!.accessor(
                                        this@getFunction,
                                        irGet(function.dispatchReceiverParameter!!)
                                    ),
                                    field
                                )
                            }
                        },
                        sourceComponent = parentNode,
                        treeElement = parentNode.treeElement!!.childField(field.name.asString()),
                        field = field
                    )
                } else {
                    val provider = declarationStore.getProvider(FqName(providerPathOrClass))

                    val bindingMetadata = provider.descriptor.annotations.single {
                        it.fqName == InjektFqNames.BindingMetadata
                    }

                    val qualifiers = bindingMetadata.getStringList("qualifiers")
                        .map { FqName(it) }

                    val createFunction = (if (provider.kind == ClassKind.OBJECT)
                        provider else provider.companionObject() as IrClass)
                        .functions
                        .single { it.name.asString() == "create" }

                    val resultType = createFunction.returnType

                    val dependencies = createFunction.valueParameters
                        .map {
                            Key(
                                it.type,
                                it.descriptor.annotations.single {
                                        it.fqName == InjektFqNames.BindingMetadata
                                    }.getStringList("qualifiers")
                                    .map { FqName(it) }
                            )
                        }

                    StatelessBinding(
                        key = Key(resultType, qualifiers),
                        dependencies = dependencies,
                        provider = provider,
                        providerInstance = {
                            if (provider.kind == ClassKind.OBJECT) {
                                irGetObject(provider.symbol)
                            } else {
                                val constructor = provider.constructors.single()
                                irCall(constructor).apply {
                                    dependencies.forEachIndexed { index, dependencyKey ->
                                        val dependency = allBindings.getValue(dependencyKey)
                                        putValueArgument(
                                            index, dependency.providerInstance(
                                                this@StatelessBinding,
                                                it
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        getFunction = getFunction(resultType) { function ->
                            if (provider.kind == ClassKind.OBJECT) {
                                irCall(
                                    provider
                                        .functions
                                        .single { it.name.asString() == "create" }
                                ).apply {
                                    dispatchReceiver = irGetObject(provider.symbol)
                                }
                            } else {
                                irCall(createFunction).apply {
                                    dispatchReceiver = irGetObject(
                                        provider.companionObject()!!.cast<IrClass>().symbol
                                    )
                                    dependencies.forEachIndexed { index, dependencyKey ->
                                        val dependency = allBindings.getValue(dependencyKey)
                                        putValueArgument(
                                            index,
                                            irCall(dependency.getFunction(this@getFunction)).apply {
                                                dispatchReceiver =
                                                    irGet(function.dispatchReceiverParameter!!)
                                            })
                                    }
                                }
                            }
                        },
                        sourceComponent = parentNode
                    )
                }
            }
            .forEach { addBinding(it) }
    }

    private fun addModule(moduleNode: ModuleNode) {
        allModules += moduleNode

        val module = moduleNode.module
        val metadata = module.descriptor.annotations.single {
            it.fqName == InjektFqNames.ModuleMetadata
        }

        val scopes = metadata.getStringList("scopes")
        scopes.forEach { addScope(FqName(it), thisComponent) }

        metadata.getStringList("parents")
            .map { parent ->
                val (key, fieldName) = parent.split("=:=")
                check(fieldName.isNotEmpty()) {
                    "Field name for $key in ${moduleNode.module.fqNameWhenAvailable}"
                }
                val component = declarationStore.getComponent(key)
                ComponentNode(
                    key = key,
                    component = component,
                    treeElement = if (fieldName.isEmpty()) null
                    else moduleNode.treeElement!!.childField(fieldName)
                )
            }
            .forEach { addParent(it, true) }

        metadata.getStringList("bindings")
            .map { providerName ->
                module.declarations
                    .filterIsInstance<IrClass>()
                    .first { it.name.asString() == providerName }
            }
            .map { provider ->
                val bindingMetadata = provider.descriptor.annotations.single {
                    it.fqName == InjektFqNames.BindingMetadata
                }
                val providerMetadata = provider.descriptor.annotations.single {
                    it.fqName == InjektFqNames.ProviderMetadata
                }

                val qualifiers = bindingMetadata.getStringList("qualifiers")
                    .map { FqName(it) }

                val isSingle =
                    providerMetadata.argumentValue("isSingle")!!.value as? Boolean ?: false

                val resultType = provider.functions
                    .single { it.name.asString() == "invoke" && it.valueParameters.isEmpty() }
                    .returnType
                    .substituteByName(moduleNode.typeParametersMap)

                val key = Key(resultType, qualifiers)

                val dependencies = provider.constructors.single().valueParameters
                    .filter { it.name.asString() != "module" }
                    .map {
                        Key(
                            it.type.substituteByName(moduleNode.typeParametersMap)
                                .cast<IrSimpleType>()
                                .arguments
                                .single()
                                .typeOrNull!!,
                            it.descriptor.annotations.single {
                                    it.fqName == InjektFqNames.BindingMetadata
                                }.getStringList("qualifiers")
                                .map { FqName(it) }
                        )
                    }

                val requiresModule = provider.kind != ClassKind.OBJECT
                        && provider.companionObject()!!.cast<IrClass>()
                    .functions.single { it.name.asString() == "create" }
                    .valueParameters.first().name.asString() == "module"

                if (isSingle) {
                    val currentProviderIndex =
                        context.irTrace[InjektWritableSlices.PROVIDER_INDEX, moduleNode.componentNode.component]
                            ?: 0
                    val field = moduleNode.componentNode.component.addField(
                        Name.identifier("provider_$currentProviderIndex"),
                        context.symbolTable.referenceClass(this.provider)
                            .ensureBound(this@Graph.context.irProviders)
                            .owner
                            .typeWith(resultType)
                    ).apply {
                        annotations += DeclarationIrBuilder(context, symbol).run {
                            irCallConstructor(
                                this@Graph.context.symbolTable.referenceConstructor(
                                        providerFieldMetadata.constructors.single()
                                    )
                                    .ensureBound(this@Graph.context.irProviders),
                                emptyList()
                            ).apply {
                                putValueArgument(
                                    0,
                                    irString(provider.fqNameForIrSerialization.asString())
                                )
                            }
                        }
                    }
                    context.irTrace.record(
                        InjektWritableSlices.PROVIDER_INDEX,
                        moduleNode.componentNode.component, currentProviderIndex + 1
                    )
                    val treeElement = moduleNode.componentNode.treeElement!!
                        .childField("provider_$currentProviderIndex")

                    StatefulBinding(
                        key = key,
                        dependencies = dependencies,
                        provider = provider,
                        providerInstance = {
                            irCall(
                                this@Graph.context.symbolTable.referenceClass(singleProvider)
                                    .ensureBound(this@Graph.context.irProviders)
                                    .owner
                                    .constructors
                                    .single()
                            ).apply {
                                val constructor = provider.constructors.single()
                                putValueArgument(
                                    0,
                                    irCall(constructor).apply {
                                        if (requiresModule) {
                                            putValueArgument(
                                                0,
                                                moduleNode.treeElement!!.accessor(
                                                    this@StatefulBinding,
                                                    it
                                                )
                                            )
                                        }

                                        constructor.valueParameters
                                            .drop(if (requiresModule) 1 else 0)
                                            .forEach { valueParameter ->
                                                val dependencyKey =
                                                    dependencies[valueParameter.index - if (requiresModule) 1 else 0]
                                                val dependency =
                                                    allBindings.getValue(dependencyKey)
                                                putValueArgument(
                                                    valueParameter.index,
                                                    dependency.providerInstance(
                                                        this@StatefulBinding,
                                                        it
                                                    )
                                                )
                                            }
                                    }
                                )
                            }
                        },
                        getFunction = getFunction(resultType) { function ->
                            irCall(
                                this@Graph.context.symbolTable.referenceClass(this@Graph.provider)
                                    .ensureBound(this@Graph.context.irProviders)
                                    .owner
                                    .functions
                                    .single { it.name.asString() == "invoke" }
                                    .symbol,
                                resultType
                            ).apply {
                                dispatchReceiver = treeElement.accessor(
                                    this@getFunction,
                                    irGet(function.dispatchReceiverParameter!!)
                                )
                            }
                        },
                        sourceComponent = thisComponent,
                        treeElement = treeElement,
                        field = field
                    )
                } else {
                    StatelessBinding(
                        key = key,
                        dependencies = dependencies,
                        provider = provider,
                        sourceComponent = thisComponent,
                        providerInstance = {
                            if (provider.kind == ClassKind.OBJECT) {
                                irGetObject(provider.symbol)
                            } else {
                                val constructor = provider.constructors
                                    .single()
                                irCall(constructor).apply {
                                    if (requiresModule) {
                                        putValueArgument(
                                            0,
                                            moduleNode.treeElement!!.accessor(
                                                this@StatelessBinding,
                                                it
                                            )
                                        )
                                    }

                                    constructor.valueParameters
                                        .drop(if (requiresModule) 1 else 0)
                                        .forEach { valueParameter ->
                                            val dependencyKey =
                                                dependencies[valueParameter.index - if (requiresModule) 1 else 0]
                                            val dependency = allBindings.getValue(dependencyKey)
                                            putValueArgument(
                                                valueParameter.index,
                                                dependency.providerInstance(
                                                    this@StatelessBinding,
                                                    it
                                                )
                                            )
                                        }
                                }
                            }
                        },
                        getFunction = getFunction(resultType) { function ->
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
                                val createFunction = companion
                                    .functions
                                    .single { it.name.asString() == "create" }
                                irCall(createFunction).apply {
                                    dispatchReceiver = irGetObject(companion.symbol)

                                    if (requiresModule) {
                                        putValueArgument(
                                            0,
                                            moduleNode.treeElement!!.accessor(
                                                this@getFunction,
                                                irGet(function.dispatchReceiverParameter!!)
                                            )
                                        )
                                    }

                                    createFunction.valueParameters
                                        .drop(if (requiresModule) 1 else 0)
                                        .forEach { valueParameter ->
                                            val dependencyKey =
                                                dependencies[valueParameter.index - if (requiresModule) 1 else 0]
                                            val dependency = allBindings.getValue(dependencyKey)
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
                        }
                    )
                }
            }.forEach { addBinding(it) }

        metadata.getStringList("includedModules").forEach { includedModuleName ->
            val field = module.fields.single { it.name.asString() == includedModuleName }
            val includedModule =
                declarationStore.getModule(field.type.classOrNull!!.descriptor.fqNameSafe)
            val typeParametersMap = includedModule.typeParameters
                .map { it.symbol to (field.type as IrSimpleType).arguments[it.index].typeOrNull!! }
                .toMap()
            ModuleNode(
                module = includedModule,
                componentNode = moduleNode.componentNode,
                typeParametersMap = typeParametersMap,
                treeElement = if (includedModuleName.isEmpty()) null
                else moduleNode.treeElement!!.childField(includedModuleName)
            ).also { addModule(it) }
        }
    }

    private fun addBinding(binding: Binding) {
        check(!binding.key.type.isTypeParameter()) {
            "Binding type not refined ${binding.key}"
        }

        check(binding.key !in allBindings) {
            "Duplicated binding ${binding.key}"
        }

        allBindings[binding.key] = binding
        if (binding.sourceComponent == thisComponent) {
            thisBindings[binding.key] = binding
        }
    }

    private fun validate() {
        // check
        allBindings.forEach { (_, binding) ->
            binding.dependencies.forEach { dependency ->
                check(dependency in allBindings) {
                    "Missing binding for $dependency."
                }
            }
        }
    }

    private fun getFunction(
        resultType: IrType,
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

            isInline = true
            returnType = resultType
            visibility = Visibilities.PUBLIC
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
}
