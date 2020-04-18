package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asTypeName
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getConstant
import com.ivianuu.injekt.compiler.getModuleName
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ModuleTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore,
    private val moduleFragment: IrModuleFragment
) : AbstractInjektTransformer(pluginContext) {

    private val moduleMetadata = getTopLevelClass(InjektFqNames.ModuleMetadata)
    private val provider = getTopLevelClass(InjektFqNames.Provider)
    private val providerDsl = getTopLevelClass(InjektFqNames.ProviderDsl)
    private val providerMetadata = getTopLevelClass(InjektFqNames.ProviderMetadata)

    private val moduleFunctions = mutableListOf<IrFunction>()
    private val processedModules = mutableMapOf<IrFunction, IrClass>()
    private val processingModules = mutableSetOf<FqName>()
    private var computedModuleFunctions = false

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        computeModuleFunctionsIfNeeded()

        moduleFunctions.forEach { function ->
            DeclarationIrBuilder(pluginContext, function.symbol).run {
                getProcessedModule(function)
            }
        }

        return super.visitModuleFragment(declaration)
    }

    fun getProcessedModule(fqName: FqName): IrClass? {
        processedModules.values.firstOrNull {
            it.fqNameForIrSerialization == fqName
        }?.let { return it }

        val function = moduleFunctions.firstOrNull {
            getModuleName(it.descriptor) == fqName
        } ?: return null

        return getProcessedModule(function)
    }

    fun getProcessedModule(function: IrFunction): IrClass? {
        computeModuleFunctionsIfNeeded()

        check(function in moduleFunctions) {
            "Unknown function $function"
        }
        processedModules[function]?.let { return it }
        return DeclarationIrBuilder(pluginContext, function.symbol).run {
            val moduleFqName =
                getModuleName(function.descriptor)
            check(moduleFqName !in processingModules) {
                "Circular dependency for module $moduleFqName"
            }
            processingModules += moduleFqName
            val moduleClass = moduleClass(function)
            function.file.addChild(moduleClass)
            function.body = irExprBody(irInjektStubUnit())
            processedModules[function] = moduleClass
            processingModules -= moduleFqName
            moduleClass
        }
    }

    private fun computeModuleFunctionsIfNeeded() {
        if (computedModuleFunctions) return
        computedModuleFunctions = true
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Module)) {
                    moduleFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })
    }

    private fun IrBuilderWithScope.moduleClass(
        function: IrFunction
    ): IrClass {
        return buildClass {
            kind = ClassKind.CLASS
            origin =
                InjektDeclarationOrigin
            name = getModuleName(function.descriptor)
                .shortName()
            modality = Modality.FINAL
            visibility = function.visibility
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            val scopeCalls = mutableListOf<IrCall>()
            val parentCalls = mutableListOf<IrCall>()
            val definitionCalls = mutableListOf<IrCall>()
            val moduleCalls = mutableListOf<IrCall>()

            function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    super.visitCall(expression)

                    when {
                        expression.symbol.descriptor.name.asString() == "scope" -> {
                            scopeCalls += expression
                        }
                        expression.symbol.descriptor.name.asString() == "parent" -> {
                            parentCalls += expression
                        }
                        expression.symbol.descriptor.name.asString() == "factory" -> {
                            definitionCalls += expression
                        }
                        expression.symbol.descriptor.name.asString() == "single" -> {
                            definitionCalls += expression
                        }
                        expression.symbol.descriptor.name.asString() == "bind" -> {
                        }
                        expression.symbol.descriptor.name.asString() == "instance" -> {
                            definitionCalls += expression
                        }
                        expression.symbol.descriptor.annotations.hasAnnotation(
                            InjektFqNames.Module
                        )
                        -> {
                            moduleCalls += expression
                        }
                    }

                    return expression
                }
            })

            val scopes = mutableListOf<FqName>()

            scopeCalls.forEach {
                val fqName = it.getTypeArgument(0)!!
                    .toKotlinType().constructor.declarationDescriptor!!.fqNameSafe
                check(fqName !in scopes) {
                    "Duplicated scope $fqName"
                }

                scopes += fqName
            }

            val parentsByCalls = mutableMapOf<IrCall, IrClass>()
            val parentKeys = mutableSetOf<String>()

            parentCalls.forEach {
                val key = it.getValueArgument(0)!!.getConstant<String>()
                check(key !in parentKeys) {
                    "Duplicated parent $key"
                }
                parentsByCalls[it] = declarationStore.getComponent(key)
            }

            val parentFields = parentsByCalls.values.toList().associateWith {
                addField(
                    it.name.asString(),
                    it.defaultType,
                    Visibilities.PUBLIC
                )
            }

            val modulesByCalls = mutableMapOf<IrCall, IrClass>()

            moduleCalls.forEach {
                val moduleFqName = getModuleName(it.symbol.descriptor)
                modulesByCalls[it] = declarationStore.getModule(moduleFqName)
            }

            val moduleFields = mutableMapOf<IrClass, IrField>()

            modulesByCalls.toList().forEachIndexed { index, (call, module) ->
                moduleFields[module] = addField(
                    "module_$index",
                    module.typeWith((0 until call.typeArgumentsCount)
                        .map { call.getTypeArgument(it)!! }
                    ),
                    Visibilities.PRIVATE
                )
            }

            var parameterMap = emptyMap<IrValueParameter, IrValueParameter>()
            var fieldsByParameters = emptyMap<IrValueParameter, IrField>()

            copyTypeParametersFrom(function)

            addConstructor {
                returnType = defaultType
                visibility = Visibilities.PUBLIC
                isPrimary = true
            }.apply {
                copyTypeParametersFrom(this@clazz)

                parameterMap = function.valueParameters
                    .associateWith { it.copyTo(this) }
                valueParameters = parameterMap.values.toList()
                fieldsByParameters = valueParameters.associateWith {
                    addField {
                        this.name = it.name
                        type = it.type
                    }
                }

                body = irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(
                            context.builtIns.any
                                .unsubstitutedPrimaryConstructor!!
                        )
                    )
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )

                    fieldsByParameters.forEach { (parameter, field) ->
                        +irSetField(
                            irGet(thisReceiver!!),
                            field,
                            irGet(parameter)
                        )
                    }

                    parentsByCalls.forEach { (call, parent) ->
                        +irSetField(
                            irGet(thisReceiver!!),
                            parentFields.getValue(parent),
                            irCall(parent.constructors.single())
                        )
                    }

                    modulesByCalls.forEach { (call, module) ->
                        moduleFields[module]?.let { field ->
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
            }

            val providerByDefinitionCall = mutableMapOf<IrCall, IrClass>()

            definitionCalls.forEachIndexed { index, definitionCall ->
                addChild(
                    provider(
                        name = Name.identifier("provider_$index"),
                        definition = definitionCall.getValueArgument(0)!!.cast(),
                        isSingle = definitionCall.symbol.descriptor.name.asString() == "single",
                        module = this,
                        moduleParametersMap = parameterMap,
                        moduleFieldsByParameter = fieldsByParameters
                    ).also { providerByDefinitionCall[definitionCall] = it }
                )
            }

            annotations += moduleMetadata(
                scopes = scopes,
                parentCalls = parentCalls,
                parentFields = parentFields,
                definitionCalls = definitionCalls,
                providerByDefinitionCall = providerByDefinitionCall,
                modulesByCalls = modulesByCalls
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
    }

    private fun IrBuilderWithScope.moduleMetadata(
        scopes: List<FqName>,
        parentCalls: List<IrCall>,
        parentFields: Map<IrClass, IrField>,
        definitionCalls: List<IrCall>,
        providerByDefinitionCall: Map<IrCall, IrClass>,
        modulesByCalls: Map<IrCall, IrClass>
    ): IrConstructorCall {
        return irCallConstructor(
            symbolTable.referenceConstructor(moduleMetadata.constructors.single())
                .ensureBound(pluginContext.irProviders),
            emptyList()
        ).apply {
            // scopes
            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    scopes.map { irString(it.asString()) }
                )
            )

            // parent keys
            putValueArgument(
                1,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    parentCalls.map {
                        irString(it.getValueArgument(0)!!.getConstant())
                    }
                )
            )

            // parent names
            putValueArgument(
                2,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    parentFields.map { irString(it.value.name.asString()) }
                )
            )

            val bindings = definitionCalls
                .map { call ->
                    irString(
                        call.getTypeArgument(0)!!.toKotlinType().asTypeName()!!
                            .toString()
                    ) to irString(
                        providerByDefinitionCall[call]!!.name.asString()
                    )
                }
            // binding keys
            putValueArgument(
                3,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    bindings.map { it.first }
                )
            )
            // binding providers
            putValueArgument(
                4,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    bindings.map { it.second }
                )
            )

            // included module types
            putValueArgument(
                5,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    modulesByCalls.values.toList()
                        .map {
                            irString(it.fqNameForIrSerialization.asString())
                        }
                )
            )

            // included module names
            putValueArgument(
                6,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    modulesByCalls.values.toList()
                        .indices
                        .map { irString("module_$it") }
                )
            )
        }
    }

    private fun IrBuilderWithScope.provider(
        name: Name,
        definition: IrFunctionExpression,
        isSingle: Boolean,
        module: IrClass,
        moduleParametersMap: Map<IrValueParameter, IrValueParameter>,
        moduleFieldsByParameter: Map<IrValueParameter, IrField>
    ): IrClass {
        val resultType = definition.function.returnType

        val definitionFunction = definition.function

        val dependencies = mutableListOf<IrCall>()

        val capturedModuleValueParameters = mutableListOf<IrValueParameter>()

        definitionFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val callee = expression.symbol.owner
                if (callee.name.asString() == "get" &&
                    (callee.extensionReceiverParameter
                        ?: callee.dispatchReceiverParameter)?.descriptor?.type
                        ?.constructor?.declarationDescriptor == providerDsl
                ) {
                    dependencies += expression
                }
                return expression
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
            kind = if (dependencies.isNotEmpty() ||
                capturedModuleValueParameters.isNotEmpty()
            ) ClassKind.CLASS else ClassKind.OBJECT
            origin =
                InjektDeclarationOrigin
            this.name = name
            modality = Modality.FINAL
            visibility = Visibilities.PUBLIC
        }.apply clazz@{
            superTypes += provider
                .defaultType
                .replace(
                    newArguments = listOf(
                        resultType.toKotlinType().asTypeProjection()
                    )
                )
                .toIrType()

            copyTypeParametersFrom(module)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val moduleField = if (capturedModuleValueParameters.isNotEmpty()) {
                addField(
                    "module",
                    module.defaultType
                )
            } else null

            var depIndex = 0
            val fieldsByDependency = dependencies
                .associateWith { expression ->
                    addField {
                        this.name = Name.identifier("p$depIndex")
                        type = symbolTable.referenceClass(provider)
                            .ensureBound(pluginContext.irProviders)
                            .typeWith(expression.type)
                        visibility = Visibilities.PRIVATE
                    }.also { depIndex++ }
                }

            addConstructor {
                returnType = defaultType
                visibility = Visibilities.PUBLIC
                isPrimary = true
            }.apply {
                copyTypeParametersFrom(this@clazz)

                if (moduleField != null) {
                    addValueParameter(
                        "module",
                        module.defaultType
                    )
                }

                fieldsByDependency.forEach { (_, field) ->
                    addValueParameter(
                        field.name.asString(),
                        field.type
                    )
                }

                body = irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(
                            context.builtIns.any
                                .unsubstitutedPrimaryConstructor!!
                        )
                    )
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )

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
                                fieldsByDependency.values.toList()
                                        [valueParameter.index - if (moduleField != null) 1 else 0],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            addFunction {
                this.name = Name.identifier("invoke")
                returnType = resultType
                visibility = Visibilities.PUBLIC
            }.apply {
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += symbolTable.referenceSimpleFunction(
                    provider.unsubstitutedMemberScope.findSingleFunction(Name.identifier("invoke"))
                )

                body = definitionFunction.body
                body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                            super.visitReturn(expression)
                        } else {
                            at(expression.startOffset, expression.endOffset)
                            DeclarationIrBuilder(
                                pluginContext,
                                symbol
                            ).irReturn(expression.value.transform(this, null)).apply {
                                this.returnTargetSymbol
                            }
                        }
                    }

                    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                        if (declaration.parent == definitionFunction)
                            declaration.parent = this@apply
                        return super.visitDeclaration(declaration)
                    }

                    override fun visitCall(expression: IrCall): IrExpression {
                        super.visitCall(expression)
                        return fieldsByDependency[expression]?.let { field ->
                            irCall(
                                symbolTable.referenceSimpleFunction(
                                    provider.findFirstFunction("invoke") { true }
                                ),
                                expression.type
                            ).apply {
                                dispatchReceiver = irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    field
                                )
                            }
                        } ?: expression
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return if (moduleParametersMap.keys.none { it.symbol == expression.symbol }) {
                            super.visitGetValue(expression)
                        } else {
                            val newParameter = moduleParametersMap[expression.symbol.owner]!!
                            val field = moduleFieldsByParameter[newParameter]!!
                            return irGetField(
                                irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    moduleField!!
                                ),
                                field
                            )
                        }
                    }
                })
            }

            annotations += providerMetadata(isSingle)
        }
    }

    private fun IrBuilderWithScope.providerMetadata(isSingle: Boolean): IrConstructorCall {
        return irCallConstructor(
            symbolTable.referenceConstructor(providerMetadata.constructors.single())
                .ensureBound(pluginContext.irProviders),
            emptyList()
        ).apply {
            putValueArgument(
                0,
                irBoolean(isSingle)
            )
        }
    }

}
