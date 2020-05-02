package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleTransformer(
    context: IrPluginContext,
    bindingTrace: BindingTrace,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context, bindingTrace) {

    private val moduleFunctions = mutableListOf<IrFunction>()
    private val transformedModules = mutableMapOf<IrFunction, IrClass>()
    private val transformingModules = mutableSetOf<FqName>()
    private var computedModuleFunctions = false

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        computeModuleFunctionsIfNeeded()

        moduleFunctions.forEach { function ->
            DeclarationIrBuilder(context, function.symbol).run {
                getGeneratedModuleClass(function)
            }
        }

        return super.visitModuleFragment(declaration)
    }

    fun getGeneratedModuleClass(fqName: FqName): IrClass? {
        transformedModules.values.firstOrNull {
            it.fqNameForIrSerialization == fqName
        }?.let { return it }

        val function = moduleFunctions.firstOrNull {
            val packageName = it.fqNameForIrSerialization.parent()
            packageName.child(
                InjektNameConventions.getModuleNameForModuleFunction(it.name)
            ) == fqName
        } ?: return null

        return getGeneratedModuleClass(function)
    }

    fun getGeneratedModuleClass(function: IrFunction): IrClass? {
        computeModuleFunctionsIfNeeded()

        check(function in moduleFunctions) {
            "Unknown function $function"
        }
        transformedModules[function]?.let { return it }
        return DeclarationIrBuilder(context, function.symbol).run {
            val packageName = function.fqNameForIrSerialization.parent()
            val moduleName =
                InjektNameConventions.getModuleNameForModuleFunction(function.name)
            val moduleFqName = packageName.child(moduleName)
            check(moduleFqName !in transformingModules) {
                "Circular dependency for module $moduleFqName"
            }
            transformingModules += moduleFqName
            val moduleClass = moduleClass(function)
            function.file.addChild(moduleClass)
            (function.file as IrFileImpl).metadata =
                MetadataSource.File(function.file.declarations.map { it.descriptor })
            function.body = irExprBody(irInjektIntrinsicUnit())
            transformedModules[function] = moduleClass
            transformingModules -= moduleFqName
            moduleClass
        }
    }

    private fun computeModuleFunctionsIfNeeded() {
        if (computedModuleFunctions) return
        computedModuleFunctions = true
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Module)
                    && (declaration.parent as? IrFunction)?.descriptor?.fqNameSafe?.asString() != "com.ivianuu.injekt.createImplementation"
                    && (declaration.parent as? IrFunction)?.descriptor?.fqNameSafe?.asString() != "com.ivianuu.injekt.createInstance"
                ) {
                    moduleFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })
    }

    private fun IrBuilderWithScope.moduleClass(
        function: IrFunction
    ) = buildClass {
        name = InjektNameConventions.getModuleNameForModuleFunction(function.name)
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        val parameterMap = mutableMapOf<IrValueParameter, IrValueParameter>()
        val fieldsByParameters = mutableMapOf<IrValueParameter, IrField>()

        copyTypeParametersFrom(function)

        val scopeCalls = mutableListOf<IrCall>()
        val dependencyCalls = mutableListOf<IrCall>()
        val childFactoryCalls = mutableListOf<IrCall>()
        val moduleCalls = mutableListOf<IrCall>()

        val aliasCalls = mutableListOf<IrCall>()
        val instanceCalls = mutableListOf<IrCall>()
        val transientCalls = mutableListOf<IrCall>()
        val scopedCalls = mutableListOf<IrCall>()

        val setCalls = mutableListOf<IrCall>()
        val mapCalls = mutableListOf<IrCall>()

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                val callee = expression.symbol.descriptor

                when {
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.scope" -> {
                        scopeCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.dependency" -> {
                        dependencyCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.childFactory" -> {
                        childFactoryCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.alias" -> {
                        aliasCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.transient" -> {
                        transientCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.scoped" -> {
                        scopedCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.instance" -> {
                        instanceCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.map" -> {
                        mapCalls += expression
                    }
                    callee.fqNameSafe.asString() == "com.ivianuu.injekt.set" -> {
                        setCalls += expression
                    }
                    expression.symbol.descriptor.annotations.hasAnnotation(InjektFqNames.Module) -> {
                        moduleCalls += expression
                    }
                }

                return expression
            }
        })

        var dependencyIndex = 0
        val dependencyFieldsByCall = dependencyCalls.associateWith { call ->
            addField(
                fieldName = "dependency_${dependencyIndex++}",
                fieldType = call.getTypeArgument(0)!!,
                fieldVisibility = Visibilities.PUBLIC
            )
        }

        val modulesByCalls = moduleCalls.associateWith { call ->
            val packageName = call.symbol.owner.fqNameForIrSerialization
                .parent()
            declarationStore.getModule(
                packageName
                    .child(InjektNameConventions.getModuleNameForModuleFunction(call.symbol.owner.name))
            )
        }

        var includeIndex = 0
        val moduleFieldsByCall = moduleCalls.associateWith { call ->
            val packageName = call.symbol.owner.fqNameForIrSerialization
                .parent()
            val moduleClass = declarationStore.getModule(
                packageName.child(
                    InjektNameConventions.getModuleNameForModuleFunction(call.symbol.owner.name)
                )
            )
            addField(
                fieldName = "module_${includeIndex++}",
                fieldType = moduleClass.typeWith((0 until call.typeArgumentsCount)
                    .map { call.getTypeArgument(it)!! }
                ),
                fieldVisibility = Visibilities.PUBLIC
            )
        }

        var instanceIndex = 0
        val instanceFieldsByCall = instanceCalls.associateWith { call ->
            val instanceQualifiers =
                context.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
            val rawType = call.getTypeArgument(0)!!
            val instanceType = (rawType as? IrSimpleType)?.buildSimpleType {
                instanceQualifiers.forEach { qualifier ->
                    annotations += noArgSingleConstructorCall(
                        symbols.getTopLevelClass(qualifier)
                    )
                }
            } ?: rawType
            addField(
                fieldName = "instance_${instanceIndex++}",
                fieldType = instanceType,
                fieldVisibility = Visibilities.PUBLIC
            )
        }

        val providerByCall = mutableMapOf<IrCall, IrClass>()

        (transientCalls + scopedCalls).forEachIndexed { index, bindingCall ->
            addChild(
                provider(
                    name = Name.identifier("provider\$$index"),
                    definition = bindingCall.getValueArgument(0)!! as IrFunctionExpression,
                    module = this,
                    moduleParametersMap = parameterMap,
                    moduleFieldsByParameter = fieldsByParameters
                ).also { providerByCall[bindingCall] = it }
            )
        }

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            copyTypeParametersFrom(this@clazz)

            function.valueParameters.forEach { p ->
                addValueParameter(
                    name = p.name.asString(),
                    type = p.type
                ).also { parameterMap[p] = it }
            }

            valueParameters.forEach { p ->
                addField(
                    "p_${p.name.asString()}",
                    p.type
                ).also { fieldsByParameters[p] = it }
            }

            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)

                fieldsByParameters.forEach { (parameter, field) ->
                    +irSetField(
                        irGet(thisReceiver!!),
                        field,
                        irGet(parameter)
                    )
                }

                instanceFieldsByCall.forEach { (call, field) ->
                    +irSetField(
                        irGet(thisReceiver!!),
                        field,
                        call.getValueArgument(0)!!
                    )
                }

                moduleFieldsByCall.forEach { (call, field) ->
                    val module = modulesByCalls.getValue(call)
                    +irSetField(
                        irGet(thisReceiver!!),
                        field,
                        irCall(module.constructors.single()).apply {
                            (0 until call.typeArgumentsCount)
                                .map { call.getTypeArgument(it)!! }
                                .forEachIndexed { index, type ->
                                    putTypeArgument(index, type)
                                }
                            copyValueArgumentsFrom(call, call.symbol.owner, symbol.owner)
                        }
                    )
                }
            }
        }

        addChild(
            moduleDescriptor(
                module = this,
                scopeCalls = scopeCalls,
                dependencyCalls = dependencyCalls,
                dependencyFieldByCall = dependencyFieldsByCall,
                childFactoryCalls = childFactoryCalls,
                moduleCalls = moduleCalls,
                modulesByCalls = modulesByCalls,
                moduleFieldByCall = moduleFieldsByCall,
                aliasCalls = aliasCalls,
                instanceCalls = instanceCalls,
                instanceFieldByCall = instanceFieldsByCall,
                transientCalls = transientCalls,
                scopedCalls = scopedCalls,
                providerByCall = providerByCall,
                setCalls = setCalls,
                mapCalls = mapCalls
            )
        )

        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return if (parameterMap.keys.none { it.symbol == expression.symbol }) {
                    super.visitGetValue(expression)
                } else {
                    val newParameter = parameterMap[expression.symbol.owner]!!
                    val field = fieldsByParameters[newParameter]!!
                    return irGetField(
                        irGet(thisReceiver!!),
                        field
                    )
                }
            }
        })
    }

    private fun IrBuilderWithScope.moduleDescriptor(
        module: IrClass,
        scopeCalls: List<IrCall>,
        dependencyCalls: List<IrCall>,
        dependencyFieldByCall: Map<IrCall, IrField>,
        childFactoryCalls: List<IrCall>,
        moduleCalls: List<IrCall>,
        modulesByCalls: Map<IrCall, IrClass>,
        moduleFieldByCall: Map<IrCall, IrField>,
        aliasCalls: List<IrCall>,
        instanceCalls: List<IrCall>,
        instanceFieldByCall: Map<IrCall, IrField>,
        transientCalls: List<IrCall>,
        scopedCalls: List<IrCall>,
        providerByCall: Map<IrCall, IrClass>,
        setCalls: List<IrCall>,
        mapCalls: List<IrCall>,
    ): IrClass = buildClass {
        kind = ClassKind.INTERFACE
        name = Name.identifier("Descriptor")
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()

        copyTypeParametersFrom(module)

        annotations += noArgSingleConstructorCall(symbols.astModule)

        // scopes
        scopeCalls.forEachIndexed { index, scopeCall ->
            val scopeType = scopeCall.getTypeArgument(0)!!
            addFunction(
                name = "scope_$index",
                returnType = scopeType,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += noArgSingleConstructorCall(symbols.astScope)
            }
        }

        // dependencies
        dependencyCalls.forEachIndexed { index, dependencyCall ->
            val dependencyType = dependencyCall.getTypeArgument(0)!!
            addFunction(
                name = "dependency_$index",
                returnType = dependencyType,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += fieldPathAnnotation(dependencyFieldByCall.getValue(dependencyCall))
                annotations += noArgSingleConstructorCall(symbols.astScope)
            }
        }

        // child factories
        childFactoryCalls.forEachIndexed { index, childFactoryCall ->
            val functionRef = childFactoryCall.getValueArgument(0)!! as IrFunctionReference
            val packageName = functionRef.symbol.owner.fqNameForIrSerialization
                .parent()
            val moduleFunctionName = InjektNameConventions.getModuleNameForFactoryBlock(
                functionRef.symbol.owner.name
            )
            val moduleClassName = InjektNameConventions.getModuleNameForModuleFunction(
                moduleFunctionName
            )
            val moduleClassFqName = packageName.child(moduleClassName)
            val childFactoryModule = declarationStore.getModuleOrNull(moduleClassFqName)

            addFunction(
                name = "child_factory_$index",
                returnType = functionRef.symbol.owner.returnType,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += noArgSingleConstructorCall(symbols.astChildFactory)
                if (childFactoryModule != null) {
                    addValueParameter(
                        "module",
                        childFactoryModule.defaultType
                    )
                }
            }
        }

        // module calls
        moduleCalls.forEachIndexed { index, moduleCall ->
            val moduleClass = modulesByCalls.getValue(moduleCall)

            addFunction(
                name = "module_$index",
                returnType = moduleClass
                    .typeWith((0 until moduleCall.typeArgumentsCount).map {
                        moduleCall.getTypeArgument(
                            it
                        )!!
                    }),
                modality = Modality.ABSTRACT
            ).apply {
                annotations += fieldPathAnnotation(moduleFieldByCall.getValue(moduleCall))
                annotations += noArgSingleConstructorCall(symbols.astModule)
            }
        }

        aliasCalls.forEachIndexed { index, aliasCall ->
            addFunction(
                name = "alias_$index",
                returnType = aliasCall.getTypeArgument(1)!!,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += noArgSingleConstructorCall(symbols.astAlias)
                addValueParameter(
                    name = "original",
                    type = aliasCall.getTypeArgument(0)!!
                )
            }
        }

        (transientCalls + instanceCalls + scopedCalls).forEachIndexed { index, bindingCall ->
            val bindingQualifiers =
                context.irTrace[InjektWritableSlices.QUALIFIERS, bindingCall] ?: emptyList()
            val rawType = bindingCall.getTypeArgument(0)!!
            val bindingType = (rawType as? IrSimpleType)?.buildSimpleType {
                bindingQualifiers.forEach { qualifier ->
                    annotations += noArgSingleConstructorCall(
                        symbols.getTopLevelClass(qualifier)
                    )
                }
            } ?: rawType

            addFunction(
                name = "binding_$index",
                returnType = bindingType,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += noArgSingleConstructorCall(symbols.astBinding)

                if (bindingCall !in instanceCalls) {
                    providerByCall.getValue(bindingCall)
                        .constructors
                        .single()
                        .valueParameters
                        .map { it to (it.type as IrSimpleType).arguments.single().typeOrNull!! }
                        .forEachIndexed { index, (valueParameter, type) ->
                            addValueParameter(
                                name = "p$index",
                                type = type
                            ).apply {
                                if (valueParameter.annotations.hasAnnotation(InjektFqNames.AstAssisted)) {
                                    annotations += noArgSingleConstructorCall(symbols.astAssisted)
                                }
                            }
                        }

                    annotations += classPathAnnotation(providerByCall.getValue(bindingCall))
                    if (bindingCall in scopedCalls) {
                        annotations += noArgSingleConstructorCall(symbols.astScoped)
                    }
                } else {
                    annotations += fieldPathAnnotation(instanceFieldByCall.getValue(bindingCall))
                }
            }
        }

        mapCalls.forEachIndexed { mapIndex, mapCall ->
            val mapQualifiers =
                context.irTrace[InjektWritableSlices.QUALIFIERS, mapCall] ?: emptyList()
            val mapKeyType = mapCall.getTypeArgument(0)!!
            val mapValueType = mapCall.getTypeArgument(1)!!

            val mapType = symbolTable.referenceClass(builtIns.map)
                .ensureBound(irProviders)
                .typeWith(mapKeyType, mapValueType)
                .buildSimpleType {
                    mapQualifiers.forEach { qualifier ->
                        annotations += noArgSingleConstructorCall(
                            symbols.getTopLevelClass(qualifier)
                        )
                    }
                }

            addFunction(
                name = "map_$mapIndex",
                returnType = mapType,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += noArgSingleConstructorCall(symbols.astMap)
            }

            val mapBlock = mapCall.getValueArgument(0) as? IrFunctionExpression
            var entryIndex = 0
            mapBlock?.function?.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.symbol == symbols.mapDsl.functions.single { it.descriptor.name.asString() == "put" }) {
                        addFunction(
                            name = "map_${mapIndex}_entry_${entryIndex++}",
                            returnType = irBuiltIns.unitType,
                            modality = Modality.ABSTRACT
                        ).apply {
                            annotations += noArgSingleConstructorCall(symbols.astMapEntry)
                            addValueParameter(
                                name = "map",
                                type = mapType
                            )
                            addValueParameter(
                                name = "entry",
                                type = expression.getTypeArgument(0)!!
                            ).apply {
                                annotations += irMapKeyConstructorForKey(
                                    expression.getValueArgument(0)!!
                                )
                            }
                        }
                    }

                    return super.visitCall(expression)
                }
            })
        }

        setCalls.forEachIndexed { setIndex, setCall ->
            val setQualifiers =
                context.irTrace[InjektWritableSlices.QUALIFIERS, setCall] ?: emptyList()
            val setElementType = setCall.getTypeArgument(0)!!

            val setType = symbolTable.referenceClass(builtIns.set)
                .ensureBound(irProviders)
                .typeWith(setElementType)
                .buildSimpleType {
                    setQualifiers.forEach { qualifier ->
                        annotations += noArgSingleConstructorCall(
                            symbols.getTopLevelClass(qualifier)
                        )
                    }
                }

            addFunction(
                name = "set_$setIndex",
                returnType = setType,
                modality = Modality.ABSTRACT
            ).apply {
                annotations += noArgSingleConstructorCall(symbols.astSet)
            }

            val setBlock = setCall.getValueArgument(0) as? IrFunctionExpression
            var elementIndex = 0

            setBlock?.function?.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.symbol == symbols.setDsl.functions.single { it.descriptor.name.asString() == "add" }) {
                        addFunction(
                            name = "set_${setIndex}_element_${elementIndex++}",
                            returnType = irBuiltIns.unitType,
                            modality = Modality.ABSTRACT
                        ).apply {
                            annotations += noArgSingleConstructorCall(symbols.astSetElement)
                            addValueParameter(
                                name = "set",
                                type = setType
                            )
                            addValueParameter(
                                name = "element",
                                type = expression.getTypeArgument(0)!!
                            )
                        }
                    }
                    return super.visitCall(expression)
                }
            })
        }
    }

    private fun IrBuilderWithScope.provider(
        name: Name,
        definition: IrFunctionExpression,
        module: IrClass,
        moduleParametersMap: Map<IrValueParameter, IrValueParameter>,
        moduleFieldsByParameter: Map<IrValueParameter, IrField>
    ): IrClass {
        val resultType = definition.function.returnType

        val assistedParameterCalls = mutableListOf<IrCall>()
        val dependencyCalls = mutableListOf<IrCall>()
        val capturedModuleValueParameters = mutableListOf<IrValueParameter>()

        definition.function.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                when ((expression.dispatchReceiver as? IrGetValue)?.symbol) {
                    definition.function.extensionReceiverParameter!!.symbol -> {
                        dependencyCalls += expression
                    }
                    definition.function.valueParameters.single().symbol -> {
                        assistedParameterCalls += expression
                    }
                }
                return super.visitCall(expression)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                super.visitGetValue(expression)

                if (moduleParametersMap.keys.any { it.symbol == expression.symbol }) {
                    capturedModuleValueParameters += expression.symbol.owner as IrValueParameter
                }

                return expression
            }
        })

        return buildClass {
            kind = if (assistedParameterCalls.isNotEmpty() || dependencyCalls.isNotEmpty()
                || capturedModuleValueParameters.isNotEmpty()
            ) ClassKind.CLASS else ClassKind.OBJECT
            this.name = name
        }.apply clazz@{
            superTypes += symbols.provider.typeWith(resultType)

            copyTypeParametersFrom(module)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val moduleField = if (capturedModuleValueParameters.isNotEmpty()) {
                addField(
                    "module",
                    module.defaultType
                )
            } else null

            val dependencies = assistedParameterCalls + dependencyCalls

            var depIndex = 0
            val fieldsByDependency = dependencies
                .associateWith { expression ->
                    addField(
                        "p${depIndex++}",
                        symbols.provider.typeWith(expression.type),
                        Visibilities.PRIVATE
                    )
                }

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                copyTypeParametersFrom(this@clazz)

                if (moduleField != null) {
                    addValueParameter(
                        "module",
                        module.defaultType
                    )
                }

                fieldsByDependency.toList().forEachIndexed { index, (call, field) ->
                    addValueParameter(
                        name = "p$index",
                        type = field.type
                    ).apply {
                        if (call in assistedParameterCalls) {
                            annotations += noArgSingleConstructorCall(symbols.astAssisted)
                        }
                    }
                }

                body = irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)

                    if (moduleField != null) {
                        +irSetField(
                            irGet(thisReceiver!!),
                            moduleField,
                            irGet(valueParameters.first())
                        )
                    }

                    valueParameters
                        .drop(if (moduleField != null) 1 else 0)
                        .forEach { valueParameter ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                fieldsByDependency.values.toList()[valueParameter.index - if (moduleField != null) 1 else 0],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            val companion = if (moduleField != null || dependencies.isNotEmpty()) {
                providerCompanion(
                    module,
                    definition,
                    assistedParameterCalls,
                    dependencyCalls,
                    capturedModuleValueParameters,
                    moduleParametersMap,
                    moduleFieldsByParameter
                ).also { addChild(it) }
            } else null

            val createFunction = if (moduleField == null && dependencies.isEmpty()) {
                createFunction(
                    this, module, definition, assistedParameterCalls, dependencyCalls,
                    capturedModuleValueParameters, moduleParametersMap, moduleFieldsByParameter
                )
            } else {
                null
            }

            addFunction {
                this.name = Name.identifier("invoke")
                returnType = resultType
                visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += symbolTable.referenceSimpleFunction(
                    symbols.provider.descriptor
                        .unsubstitutedMemberScope.findSingleFunction(Name.identifier("invoke"))
                )

                body = irExprBody(
                    irCall(companion?.functions?.single() ?: createFunction!!).apply {
                        dispatchReceiver =
                            if (companion != null) irGetObject(companion.symbol) else irGet(
                                dispatchReceiverParameter!!
                            )

                        passTypeArgumentsFrom(this@clazz)

                        if (moduleField != null) {
                            putValueArgument(
                                0,
                                irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    moduleField
                                )
                            )
                        }

                        fieldsByDependency.values.forEachIndexed { index, field ->
                            putValueArgument(
                                if (moduleField != null) index + 1 else index,
                                irCall(
                                    symbolTable.referenceFunction(
                                        symbols.provider.descriptor.unsubstitutedMemberScope.findSingleFunction(
                                            Name.identifier(
                                                "invoke"
                                            )
                                        )
                                    ),
                                    (field.type as IrSimpleType).arguments.single().typeOrNull!!
                                ).apply {
                                    dispatchReceiver = irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        field
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.providerCompanion(
        module: IrClass,
        definition: IrFunctionExpression,
        assistedParameterCalls: List<IrCall>,
        dependencyCalls: List<IrCall>,
        capturedModuleValueParameters: List<IrValueParameter>,
        moduleParametersMap: Map<IrValueParameter, IrValueParameter>,
        moduleFieldsByParameter: Map<IrValueParameter, IrField>
    ) = buildClass {
        kind = ClassKind.OBJECT
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        isCompanion = true
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }

        createFunction(
            this, module, definition, assistedParameterCalls, dependencyCalls,
            capturedModuleValueParameters, moduleParametersMap, moduleFieldsByParameter
        )
    }

    private fun IrBuilderWithScope.createFunction(
        owner: IrClass,
        module: IrClass,
        definition: IrFunctionExpression,
        assistedParameterCalls: List<IrCall>,
        dependencyCalls: List<IrCall>,
        capturedModuleValueParameters: List<IrValueParameter>,
        moduleParametersMap: Map<IrValueParameter, IrValueParameter>,
        moduleFieldsByParameter: Map<IrValueParameter, IrField>
    ): IrFunction {
        val definitionFunction = definition.function
        val resultType = definition.function.returnType

        return owner.addFunction {
            name = Name.identifier("create")
            returnType = resultType
            visibility = Visibilities.PUBLIC
        }.apply {
            copyTypeParametersFrom(module)
            dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

            val moduleParameter = if (capturedModuleValueParameters.isNotEmpty()) {
                addValueParameter(
                    "module",
                    module.defaultType
                )
            } else null

            val dependencies = assistedParameterCalls + dependencyCalls

            var depIndex = 0
            val valueParametersByDependency = dependencies
                .associateWith { call ->
                    addValueParameter(
                        "p${depIndex++}",
                        call.type
                    ).apply {
                        if (call in assistedParameterCalls) {
                            annotations += noArgSingleConstructorCall(symbols.astAssisted)
                        }
                    }
                }

            body = definitionFunction.body
            body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitReturn(expression: IrReturn): IrExpression {
                    return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                        super.visitReturn(expression)
                    } else {
                        at(expression.startOffset, expression.endOffset)
                        DeclarationIrBuilder(
                            this@ModuleTransformer.context,
                            symbol
                        ).irReturn(expression.value.transform(this, null))
                    }
                }

                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                    if (declaration.parent == definitionFunction)
                        declaration.parent = this@apply
                    return super.visitDeclaration(declaration)
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    super.visitCall(expression)
                    return valueParametersByDependency[expression]?.let { valueParameter ->
                        irGet(valueParameter)
                    } ?: expression
                }

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return if (moduleParametersMap.keys.none { it.symbol == expression.symbol }) {
                        super.visitGetValue(expression)
                    } else {
                        val newParameter = moduleParametersMap[expression.symbol.owner]!!
                        val field = moduleFieldsByParameter[newParameter]!!
                        return irGetField(
                            irGet(moduleParameter!!),
                            field
                        )
                    }
                }
            })
        }
    }
}
