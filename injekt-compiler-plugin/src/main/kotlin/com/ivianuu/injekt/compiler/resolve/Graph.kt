package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.getStringList
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
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
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
    private val symbols: InjektSymbols,
    private val thisComponent: ComponentNode,
    thisComponentModule: ModuleNode?,
    private val declarationStore: InjektDeclarationStore
) {

    private val parentScopes = mutableSetOf<FqName>()
    val thisScopes = mutableSetOf<FqName>()

    val thisBindings = mutableMapOf<Key, Binding>()
    val allBindings = mutableMapOf<Key, Binding>()

    private val allModules = mutableListOf<ModuleNode>()
    val thisModules = mutableListOf<ModuleNode>()

    private val allParents = mutableListOf<ComponentNode>()
    val thisParents = mutableListOf<ComponentNode>()

    init {
        if (thisComponentModule != null) addModule(thisComponentModule)
        addUnscopedBindings()
        addScopedBindings()
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

        val modules = mutableMapOf<String, ModuleNode>()

        metadata.getStringList("modules")
            .map { modulePath ->
                val pathNames = modulePath.split("/")
                    .filter { it.isNotEmpty() }
                var currentClass: IrClass = parentNode.component
                pathNames.fold(null) { acc: ModuleNode?, pathName: String ->
                    val newPath =
                        if (acc == null) pathName else "${acc.treeElement!!.path}/$pathName"
                    modules.getOrPut(newPath) {
                        ModuleNode(
                            module = currentClass.fields.single { it.name.asString() == pathName }
                                .type.classOrNull!!.owner
                                .also { currentClass = it },
                            parentNode,
                            emptyMap(),
                            (if (acc == null) parentNode.treeElement else acc.treeElement)
                            !!.childField(pathName)
                        )
                    }
                }!!
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

                    statefulBinding(
                        field = field,
                        provider = provider,
                        moduleNode = null,
                        providerInstance = {
                            irGetField(
                                parentNode.treeElement!!.accessor(
                                    this,
                                    it
                                ),
                                field
                            )
                        },
                        treeElement = parentNode.treeElement!!.childField(field.name.asString())
                    )
                } else {
                    val provider = declarationStore.getProvider(FqName(providerPathOrClass))

                    val moduleRequired = provider.constructors.single()
                        .valueParameters.firstOrNull()?.name?.asString() == "module"

                    val moduleIfRequired = if (!moduleRequired) null else {
                        val moduleParameter = provider.constructors.single()
                            .valueParameters.first()
                        modules.values.single { it.module.defaultType == moduleParameter.type }
                    }

                    statelessBinding(
                        provider = provider,
                        duplicateStrategy = DuplicateStrategy.Fail,
                        moduleIfRequired = moduleIfRequired,
                        sourceComponent = parentNode,
                        typeParametersMap = emptyMap()
                    )
                }
            }
            .forEach { addBinding(it) }
    }

    private fun addModule(moduleNode: ModuleNode) {
        allModules += moduleNode
        if (moduleNode.componentNode == thisComponent) {
            thisModules += moduleNode
        }

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
                val providerMetadata = provider.descriptor.annotations.single {
                    it.fqName == InjektFqNames.ProviderMetadata
                }

                val isSingle =
                    providerMetadata.argumentValue("isSingle")!!.value as? Boolean ?: false

                val resultType = provider.functions
                    .single { it.name.asString() == "invoke" && it.valueParameters.isEmpty() }
                    .returnType
                    .substituteByName(moduleNode.typeParametersMap)

                val requiresModule = provider.kind != ClassKind.OBJECT
                        && provider.companionObject()!!.cast<IrClass>()
                    .functions.single { it.name.asString() == "create" }
                    .valueParameters.first().name.asString() == "module"

                if (isSingle) {
                    val field = allocateProviderField(
                        provider,
                        resultType
                    )

                    val treeElement = moduleNode.componentNode.treeElement!!
                        .childField(field.name.asString())

                    statefulBinding(
                        field = field,
                        provider = provider,
                        moduleNode = moduleNode,
                        providerInstance = newProviderInstance(
                            provider = provider,
                            moduleIfRequired = if (requiresModule) moduleNode else null
                        ),
                        treeElement = treeElement
                    )
                } else {
                    statelessBinding(
                        provider = provider,
                        duplicateStrategy = DuplicateStrategy.Fail,
                        moduleIfRequired = if (requiresModule) moduleNode else null,
                        sourceComponent = thisComponent,
                        typeParametersMap = moduleNode.typeParametersMap
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

    private fun addUnscopedBindings() {
        declarationStore.getUnscopedProviders()
            .forEach {
                addBinding(
                    statelessBinding(
                        it,
                        DuplicateStrategy.Drop,
                        null,
                        null,
                        emptyMap()
                    )
                )
            }
    }

    private fun addScopedBindings() {
        thisScopes
            .flatMap { declarationStore.getProvidersForScope(it) }
            .forEach { provider ->
                val resultType = provider.functions
                    .single { it.name.asString() == "invoke" }
                    .returnType

                val field = allocateProviderField(
                    provider,
                    resultType
                )

                val treeElement = thisComponent.treeElement!!
                    .childField(field.name.asString())

                addBinding(
                    statefulBinding(
                        field = field,
                        provider = provider,
                        moduleNode = null,
                        providerInstance = newProviderInstance(
                            provider,
                            null
                        ),
                        treeElement = treeElement
                    )
                )
            }
    }

    private fun addBinding(binding: Binding) {
        check(!binding.key.type.isTypeParameter()) {
            "Binding type not refined ${binding.key}"
        }

        when (binding.duplicateStrategy) {
            DuplicateStrategy.Drop -> {
                if (binding.key in allBindings) return
            }
            DuplicateStrategy.Fail -> {
                check(binding.key !in allBindings) {
                    "Duplicated binding ${binding.key}"
                }
            }
            DuplicateStrategy.Override -> {
            } // do nothing
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

    private fun statefulBinding(
        field: IrField,
        provider: IrClass,
        moduleNode: ModuleNode?,
        providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
        treeElement: TreeElement
    ): StatefulBinding {
        val bindingMetadata = provider.descriptor.annotations.single {
            it.fqName == InjektFqNames.BindingMetadata
        }

        val qualifiers = bindingMetadata.getStringList("qualifiers")
            .map { FqName(it) }

        val resultType = provider.functions
            .single { it.name.asString() == "invoke" && it.valueParameters.isEmpty() }
            .returnType
            .let { if (moduleNode != null) it.substituteByName(moduleNode.typeParametersMap) else it }

        val key = Key(resultType, qualifiers)

        val dependencies = provider.constructors.single().valueParameters
            .filter { it.name.asString() != "module" }
            .map {
                Key(
                    it.type
                        .let { if (moduleNode != null) it.substituteByName(moduleNode.typeParametersMap) else it }
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

        return StatefulBinding(
            key = key,
            dependencies = dependencies,
            duplicateStrategy = DuplicateStrategy.Fail,
            provider = provider,
            providerInstance = providerInstance,
            getFunction = getFunction(resultType) { function ->
                irCall(
                    symbols.provider
                        .functions
                        .single { it.owner.name.asString() == "invoke" },
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
    }

    private fun statelessBinding(
        provider: IrClass,
        duplicateStrategy: DuplicateStrategy,
        moduleIfRequired: ModuleNode? = null,
        sourceComponent: ComponentNode?,
        typeParametersMap: Map<IrTypeParameterSymbol, IrType>
    ): StatelessBinding {
        val bindingMetadata = provider.descriptor.annotations.single {
            it.fqName == InjektFqNames.BindingMetadata
        }

        val qualifiers = bindingMetadata.getStringList("qualifiers")
            .map { FqName(it) }

        val createFunction = (if (provider.kind == ClassKind.OBJECT)
            provider else provider.companionObject() as IrClass)
            .functions
            .single { it.name.asString() == "create" }

        val resultType = provider.functions
            .single { it.name.asString() == "invoke" && it.valueParameters.isEmpty() }
            .returnType
            .substituteByName(typeParametersMap)

        val key = Key(resultType, qualifiers)

        val dependencies = provider.constructors.single().valueParameters
            .filter { it.name.asString() != "module" }
            .map {
                Key(
                    it.type.substituteByName(typeParametersMap)
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

        return StatelessBinding(
            key = key,
            dependencies = dependencies,
            duplicateStrategy = duplicateStrategy,
            provider = provider,
            providerInstance = {
                if (provider.kind == ClassKind.OBJECT) {
                    irGetObject(provider.symbol)
                } else {
                    val constructor = provider.constructors.single()
                    irCall(constructor).apply {
                        if (moduleIfRequired != null) {
                            putValueArgument(
                                0,
                                moduleIfRequired.treeElement!!.accessor(
                                    this@StatelessBinding,
                                    it
                                )
                            )
                        }

                        try {
                            constructor.valueParameters
                                .drop(if (moduleIfRequired != null) 1 else 0)
                                .forEach { valueParameter ->
                                    val dependencyKey =
                                        dependencies[valueParameter.index - if (moduleIfRequired != null) 1 else 0]
                                    val dependency = allBindings.getValue(dependencyKey)
                                    putValueArgument(
                                        valueParameter.index,
                                        dependency.providerInstance(
                                            this@StatelessBinding,
                                            it
                                        )
                                    )
                                }
                        } catch (e: Exception) {
                            error("${constructor.dump()} module $moduleIfRequired")
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
                    irCall(createFunction).apply {
                        dispatchReceiver = irGetObject(companion.symbol)

                        if (moduleIfRequired != null) {
                            putValueArgument(
                                0,
                                moduleIfRequired.treeElement!!.accessor(
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
            },
            sourceComponent = sourceComponent
        )
    }

    private fun getFunction(
        resultType: IrType,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrBuilderWithScope.() -> IrFunction {
        val function = thisComponent.component.addFunction {
            val currentGetFunctionIndex =
                context.irTrace[InjektWritableSlices.GET_FUNCTION_INDEX, thisComponent.component]
                    ?: 0
            this.name = Name.identifier("get\$$currentGetFunctionIndex")
            context.irTrace.record(
                InjektWritableSlices.GET_FUNCTION_INDEX,
                thisComponent.component,
                currentGetFunctionIndex + 1
            )

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

    private fun newProviderInstance(
        provider: IrClass,
        moduleIfRequired: ModuleNode?
    ): IrBuilderWithScope.(IrExpression) -> IrExpression = func@{
        val constructor = provider.constructors.single()
        val newProvider = irCall(constructor).apply {
            if (moduleIfRequired != null) {
                putValueArgument(
                    0,
                    moduleIfRequired.treeElement!!.accessor(
                        this@func,
                        it
                    )
                )
            }

            val dependencies = provider.constructors.single().valueParameters
                .filter { it.name.asString() != "module" }
                .map {
                    Key(
                        it.type
                            .let {
                                if (moduleIfRequired != null)
                                    it.substituteByName(moduleIfRequired.typeParametersMap)
                                else it
                            }
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

            constructor.valueParameters
                .drop(if (moduleIfRequired != null) 1 else 0)
                .forEach { valueParameter ->
                    val dependencyKey =
                        dependencies[valueParameter.index - if (moduleIfRequired != null) 1 else 0]
                    val dependency =
                        allBindings.getValue(dependencyKey)
                    putValueArgument(
                        valueParameter.index,
                        dependency.providerInstance(
                            this@func,
                            it
                        )
                    )
                }
        }

        val providerMetadata = provider.descriptor.annotations.single {
            it.fqName == InjektFqNames.ProviderMetadata
        }

        val isSingle =
            providerMetadata.argumentValue("isSingle")!!.value as? Boolean ?: false

        if (isSingle) {
            irCall(
                symbols.singleProvider
                    .constructors
                    .single()
            ).apply { putValueArgument(0, newProvider) }
        } else {
            newProvider
        }
    }

    private fun allocateProviderField(
        provider: IrClass,
        resultType: IrType
    ): IrField {
        val currentProviderIndex =
            context.irTrace[InjektWritableSlices.PROVIDER_INDEX, thisComponent.component]
                ?: 0
        val field = thisComponent.component.addField(
            Name.identifier("provider\$$currentProviderIndex"),
            symbols.provider.owner.typeWith(resultType),
            Visibilities.PUBLIC
        ).apply {
            annotations += DeclarationIrBuilder(context, symbol).run {
                irCallConstructor(
                    symbols.providerFieldMetadata.constructors.single(),
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
            thisComponent.component, currentProviderIndex + 1
        )

        return field
    }
}
