package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext

class AnnotatedBindingTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    /**

    override fun visitClass(declaration: IrClass): IrStatement {
    if (declaration.annotations.hasAnnotation(InjektFqNames.Factory) ||
    declaration.annotations.hasAnnotation(InjektFqNames.Single)) {
    classes += declaration
    }
    return super.visitClass(declaration)
    }

    private fun IrBuilderWithScope.provider(
    clazz: IrClass,
    isSingle: Boolean
    ): IrClass {
    val constructor = clazz.primaryConstructor!! // todo

    return buildClass {
    kind = if (constructor.valueParameters.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
    origin = InjektDeclarationOrigin
    this.name = getProviderFqName(clazz.descriptor).shortName()
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
    addField(
    "p$depIndex",
    symbolTable.referenceClass(provider)
    .ensureBound(this@AnnotatedBindingTransformer.context.irProviders)
    .typeWith(expression.type),
    Visibilities.PRIVATE
    ).also { depIndex++ }
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

    fieldsByDependency.forEach { (call, field) ->
    addValueParameter(
    field.name.asString(),
    field.type
    ).apply {
    annotations += bindingMetadata(
    call.getValueArgument(0)
    ?.safeAs<IrVarargImpl>()
    ?.elements
    ?.filterIsInstance<IrGetObjectValue>()
    ?.map {
    it.type.classOrNull!!.descriptor.containingDeclaration
    .fqNameSafe
    } ?: emptyList()
    )
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
    dependencies,
    capturedModuleValueParameters,
    moduleParametersMap,
    moduleFieldsByParameter
    ).also { addChild(it) }
    } else null

    val createFunction = if (moduleField == null && dependencies.isEmpty()) {
    createFunction(
    this, module, definition, dependencies,
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
    provider.unsubstitutedMemberScope.findSingleFunction(Name.identifier("invoke"))
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
    provider.unsubstitutedMemberScope.findSingleFunction(
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

    annotations += bindingMetadata(qualifiers)
    annotations += providerMetadata(isSingle)
    }
    }

    private fun IrBuilderWithScope.providerCompanion(
    module: IrClass,
    definition: IrFunctionExpression,
    dependencies: MutableList<IrCall>,
    capturedModuleValueParameters: List<IrValueParameter>,
    moduleParametersMap: Map<IrValueParameter, IrValueParameter>,
    moduleFieldsByParameter: Map<IrValueParameter, IrField>
    ): IrClass {
    return buildClass {
    kind = ClassKind.OBJECT
    origin = InjektDeclarationOrigin
    name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
    modality = Modality.FINAL
    visibility = Visibilities.PUBLIC
    isCompanion = true
    }.apply clazz@{
    createImplicitParameterDeclarationWithWrappedDescriptor()

    addConstructor {
    returnType = defaultType
    visibility = Visibilities.PUBLIC
    isPrimary = true
    }.apply {
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
    }
    }

    createFunction(
    this, module, definition, dependencies,
    capturedModuleValueParameters, moduleParametersMap, moduleFieldsByParameter
    )
    }
    }

    private fun IrBuilderWithScope.createFunction(
    owner: IrClass,
    module: IrClass,
    definition: IrFunctionExpression,
    dependencies: MutableList<IrCall>,
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

    var depIndex = 0
    val valueParametersByDependency = dependencies
    .associateWith { expression ->
    addValueParameter {
    this.name = Name.identifier("p$depIndex")
    type = expression.type
    }.apply {
    annotations += bindingMetadata(
    expression.getValueArgument(0)
    ?.safeAs<IrVarargImpl>()
    ?.elements
    ?.filterIsInstance<IrGetObjectValue>()
    ?.map {
    it.type.classOrNull!!.descriptor.containingDeclaration
    .fqNameSafe
    } ?: emptyList()
    )
    }
    .also { depIndex++ }
    }

    body = definitionFunction.body
    body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
    override fun visitReturn(expression: IrReturn): IrExpression {
    return if (expression.returnTargetSymbol != definitionFunction.symbol) {
    super.visitReturn(expression)
    } else {
    at(expression.startOffset, expression.endOffset)
    DeclarationIrBuilder(
    this@AnnotatedBindingTransformer.context,
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
    }*/
}
