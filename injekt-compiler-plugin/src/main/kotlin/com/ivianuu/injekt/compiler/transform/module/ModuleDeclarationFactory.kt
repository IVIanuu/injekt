package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.ClassPath
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.Path
import com.ivianuu.injekt.compiler.PropertyPath
import com.ivianuu.injekt.compiler.TypeParameterPath
import com.ivianuu.injekt.compiler.ValueParameterPath
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ModuleDeclarationFactory(
    private val module: ModuleImplementation,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val nameProvider: NameProvider,
    private val declarationStore: InjektDeclarationStore,
    private val providerFactory: ModuleProviderFactory
) {

    private val moduleClass get() = module.clazz

    fun create(call: IrCall): List<ModuleDeclaration> {
        val callee = call.symbol.descriptor

        val declarations = mutableListOf<ModuleDeclaration>()

        when {
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.scope" ->
                declarations += createScopeDeclaration(call)
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.dependency" ->
                declarations += createDependencyDeclaration(call)
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.childFactory" ->
                declarations += createChildFactoryDeclaration(call)
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.alias" ->
                declarations += createAliasDeclaration(call)
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.transient" ||
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.scoped" ||
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.instance" ->
                declarations += createBindingDeclaration(call)
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.map" ->
                declarations += createMapDeclarations(call)
            callee.fqNameSafe.asString() == "com.ivianuu.injekt.set" ->
                declarations += createSetDeclarations(call)
            call.symbol.descriptor.annotations.hasAnnotation(InjektFqNames.Module) ->
                declarations += createIncludedModuleDeclarations(call)
        }

        return declarations
    }

    private fun createScopeDeclaration(call: IrCall): ScopeDeclaration =
        ScopeDeclaration(call.getTypeArgument(0)!!)

    private fun createDependencyDeclaration(call: IrCall): DependencyDeclaration {
        val dependencyType = call.getTypeArgument(0)!!

        val property = InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
            .fieldBakedProperty(
                moduleClass,
                Name.identifier(nameProvider.allocate("dependency")),
                dependencyType
            )

        val path = PropertyPath(property)

        return DependencyDeclaration(dependencyType, path) {
            irSetField(
                it(),
                property.backingField!!,
                call.getValueArgument(0)!!
            )
        }
    }

    private fun createChildFactoryDeclaration(call: IrCall): ChildFactoryDeclaration {
        val factoryRef = call.getValueArgument(0)!! as IrFunctionReference
        val factoryModuleClass = declarationStore.getModuleClassOrNull(
            declarationStore.getModuleFunctionForFactory(factoryRef.symbol.owner)
        )
        return ChildFactoryDeclaration(factoryRef, factoryModuleClass)
    }

    private fun createAliasDeclaration(call: IrCall): AliasDeclaration =
        AliasDeclaration(call.getTypeArgument(0)!!, call.getTypeArgument(1)!!)

    private fun createBindingDeclaration(call: IrCall): BindingDeclaration {
        val bindingQualifiers =
            pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
        val bindingType = call.getTypeArgument(0)!!
            .withAnnotations(bindingQualifiers)

        val singleArgument = if (call.valueArgumentsCount != 0)
            call.getValueArgument(0) else null

        val bindingPath: Path
        val inline: Boolean
        val parameters =
            mutableListOf<InjektDeclarationIrBuilder.ProviderParameter>()
        val statement: (IrBuilderWithScope.(() -> IrExpression) -> IrStatement)?

        fun addParametersFromProvider(provider: IrClass) {
            val assisted = provider.functions
                .single { it.name.asString() == "invoke" }
                .valueParameters
                .map { it.type }

            val nonAssisted = provider.constructors
                .single()
                .valueParameters
                .filter { it.name.asString() != "module" }
                .map { it.type.typeArguments.single() }

            parameters += (assisted + nonAssisted).mapIndexed { index, type ->
                InjektDeclarationIrBuilder.ProviderParameter(
                    name = "p$index",
                    type = type,
                    assisted = type in assisted,
                    requirement = false
                )
            }
        }

        if (singleArgument == null) {
            if (bindingType.toKotlinType().isTypeParameter()) {
                bindingPath = TypeParameterPath(
                    module.function.typeParameters.single {
                        it.descriptor ==
                                bindingType.toKotlinType().constructor.declarationDescriptor
                    }
                )
                inline = true
                statement = null
            } else {
                val provider = providerFactory.providerForClass(
                    name = Name.identifier(nameProvider.allocate("Factory")),
                    clazz = bindingType.classOrFail
                        .ensureBound(pluginContext.irProviders).owner,
                    visibility = module.clazz.visibility
                )
                module.clazz.addChild(provider)
                addParametersFromProvider(provider)
                bindingPath = ClassPath(provider)
                inline = false
                statement = null
            }
        } else {
            if (call.symbol.descriptor.name.asString() == "instance") {
                val property = InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
                    .fieldBakedProperty(
                        moduleClass,
                        Name.identifier(nameProvider.allocate("instance")),
                        bindingType
                    )

                statement = {
                    irSetField(
                        it(),
                        property.backingField!!,
                        singleArgument
                    )
                }
                bindingPath = PropertyPath(property)
                inline = false
            } else {
                when (singleArgument) {
                    is IrFunctionExpression -> {
                        val provider = providerFactory.providerForDefinition(
                            name = Name.identifier(nameProvider.allocate("Factory")),
                            definition = singleArgument,
                            visibility = module.clazz.visibility,
                            moduleFieldsByParameter = module.fieldsByParameters
                        )
                        module.clazz.addChild(provider)
                        addParametersFromProvider(provider)
                        bindingPath = ClassPath(provider)
                        inline = false
                        statement = null
                    }
                    is IrGetValue -> {
                        bindingPath = ValueParameterPath(
                            module.function.valueParameters.single {
                                it.symbol == singleArgument.symbol
                            }
                        )
                        inline = true
                        statement = null
                    }
                    else -> error("Unexpected definition ${singleArgument.dump()}")
                }
            }
        }

        return BindingDeclaration(
            bindingType = bindingType,
            parameters = parameters,
            scoped = call.symbol.descriptor.name.asString() == "scoped",
            inline = inline,
            path = bindingPath,
            statement = statement
        )
    }

    private fun createMapDeclarations(call: IrCall): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()
        val mapQualifiers =
            pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
        val mapKeyType = call.getTypeArgument(0)!!
        val mapValueType = call.getTypeArgument(1)!!

        val mapType = pluginContext.symbolTable.referenceClass(pluginContext.builtIns.map)
            .ensureBound(pluginContext.irProviders)
            .typeWith(mapKeyType, mapValueType)
            .withAnnotations(mapQualifiers)

        declarations += MapDeclaration(mapType)

        val mapBlock = call.getValueArgument(0) as? IrFunctionExpression
        mapBlock?.function?.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol == symbols.mapDsl.functions.single { it.descriptor.name.asString() == "put" }) {
                    declarations += MapEntryDeclaration(
                        mapType,
                        expression.getValueArgument(0)!!,
                        expression.getTypeArgument(0)!!
                    )
                }

                return super.visitCall(expression)
            }
        })

        return declarations
    }

    private fun createSetDeclarations(call: IrCall): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()
        val setQualifiers =
            pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
        val setElementType = call.getTypeArgument(0)!!

        val setType = pluginContext.symbolTable.referenceClass(pluginContext.builtIns.set)
            .ensureBound(pluginContext.irProviders)
            .typeWith(setElementType)
            .withAnnotations(setQualifiers)

        declarations += SetDeclaration(setType)

        val setBlock = call.getValueArgument(0) as? IrFunctionExpression
        setBlock?.function?.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol == symbols.setDsl.functions.single { it.descriptor.name.asString() == "add" }) {
                    declarations += SetElementDeclaration(
                        setType,
                        expression.getTypeArgument(0)!!
                    )
                }
                return super.visitCall(expression)
            }
        })

        return declarations
    }

    private fun createIncludedModuleDeclarations(call: IrCall): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()
        val includedClass = declarationStore.getModuleClass(call.symbol.owner)
        val includedType = includedClass.typeWith((0 until call.typeArgumentsCount)
            .map { call.getTypeArgument(it)!! })
        val property = InjektDeclarationIrBuilder(pluginContext, includedClass.symbol)
            .fieldBakedProperty(
                moduleClass,
                Name.identifier(nameProvider.allocate("module")),
                includedType
            )

        declarations += IncludedModuleDeclaration(
            includedType,
            PropertyPath(property)
        ) {
            val constructor = includedClass.constructors.single()
            irSetField(
                it(),
                property.backingField!!,
                irCall(constructor).apply {
                    (0 until call.typeArgumentsCount)
                        .map { call.getTypeArgument(it)!! }
                        .forEachIndexed { index, type ->
                            putTypeArgument(index, type)
                        }

                    var argIndex = 0
                    if (call.dispatchReceiver != null) {
                        putValueArgument(
                            argIndex++,
                            call.dispatchReceiver!!
                        )
                    }
                    if (call.extensionReceiver != null) {
                        putValueArgument(
                            argIndex++,
                            call.extensionReceiver!!
                        )
                    }

                    (0 until call.valueArgumentsCount)
                        .map { call.getValueArgument(it)!! }
                        .filter {
                            !it.type.isFunction() ||
                                    it.type.typeArguments.firstOrNull()?.classOrNull != symbols.providerDsl
                        }
                        .forEach { arg ->
                            putValueArgument(argIndex++, arg)
                        }
                }
            )
        }

        declarations += includedClass
            .declarations.single {
                it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
            }
            .let { it as IrClass }
            .functions
            .filter { it.descriptor.annotations.hasAnnotation(InjektFqNames.AstInline) }
            .filter { it.descriptor.annotations.hasAnnotation(InjektFqNames.AstBinding) }
            .map { bindingFunction ->
                val bindingType: IrType
                val bindingPath: Path
                val inline: Boolean
                val parameters =
                    mutableListOf<InjektDeclarationIrBuilder.ProviderParameter>()

                fun addParametersFromProvider(provider: IrClass) {
                    val assisted = provider.functions
                        .single { it.name.asString() == "invoke" }
                        .valueParameters
                        .map { it.type }

                    val nonAssisted = provider.constructors
                        .single()
                        .valueParameters
                        .map { it.type.typeArguments.single() }

                    parameters += (assisted + nonAssisted).mapIndexed { index, type ->
                        InjektDeclarationIrBuilder.ProviderParameter(
                            name = "p$index",
                            type = type,
                            assisted = type in assisted,
                            requirement = false
                        )
                    }
                }

                if (bindingFunction.hasAnnotation(InjektFqNames.AstTypeParameterPath)) {
                    bindingType =
                        bindingFunction.getAnnotation(InjektFqNames.AstTypeParameterPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }.value
                            .let { typeParameterName ->
                                val index = includedClass.typeParameters
                                    .indexOfFirst { it.name.asString() == typeParameterName }
                                call.getTypeArgument(index)!!
                            }
                            .withAnnotations(
                                pluginContext, moduleClass.symbol,
                                bindingFunction.returnType.toKotlinType().annotations.toList()
                            )

                    if (!bindingType.toKotlinType().isTypeParameter()) {
                        val provider = providerFactory.providerForClass(
                            name = Name.identifier(nameProvider.allocate("Factory")),
                            clazz = bindingType.classOrFail
                                .ensureBound(pluginContext.irProviders).owner,
                            visibility = module.clazz.visibility
                        )
                        module.clazz.addChild(provider)
                        addParametersFromProvider(provider)
                        bindingPath = ClassPath(provider)
                        inline = false
                    } else {
                        bindingPath = TypeParameterPath(
                            moduleClass.typeParameters.single {
                                it.name == bindingType.toKotlinType().constructor.declarationDescriptor!!.name
                            }
                        )
                        inline = true
                    }
                } else {
                    val definitionExpression =
                        bindingFunction.getAnnotation(InjektFqNames.AstValueParameterPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }.value
                            .let { valueParameterName ->
                                val index = call.symbol
                                    .ensureBound(pluginContext.irProviders)
                                    .owner
                                    .valueParameters
                                    .indexOfFirst { it.name.asString() == valueParameterName }
                                call.getValueArgument(index)
                            }
                    bindingType = definitionExpression!!.type.typeArguments.last()
                        .withAnnotations(
                            pluginContext, moduleClass.symbol,
                            bindingFunction.returnType.toKotlinType().annotations.toList()
                        )

                    if (definitionExpression is IrFunctionExpression) {
                        val provider = providerFactory.providerForDefinition(
                            name = Name.identifier(nameProvider.allocate("Factory")),
                            definition = definitionExpression,
                            visibility = module.clazz.visibility,
                            moduleFieldsByParameter = module.fieldsByParameters
                        )
                        module.clazz.addChild(provider)
                        addParametersFromProvider(provider)
                        bindingPath = ClassPath(provider)
                        inline = false
                    } else {
                        definitionExpression as IrGetValue
                        bindingPath = ValueParameterPath(
                            module.function.valueParameters.single {
                                it.symbol == definitionExpression.symbol
                            }
                        )
                        inline = true
                    }
                }

                BindingDeclaration(
                    bindingType = bindingType,
                    parameters = parameters,
                    scoped = bindingFunction.hasAnnotation(InjektFqNames.AstScoped),
                    inline = inline,
                    path = bindingPath,
                    statement = null
                )
            }

        return declarations
    }

}
