package com.ivianuu.injekt.compiler.transform

/**
class DefinitionToProviderTransformer(pluginContext: IrPluginContext) :
AbstractInjektTransformer(pluginContext) {

private val provider = getTopLevelClass(InjektFqNames.Provider)
private val providerDefinition = getTypeAlias(InjektFqNames.ProviderDefinition)
private val providerDsl = getTopLevelClass(InjektFqNames.ProviderDsl)
private val providerMetadata = getTopLevelClass(InjektFqNames.ProviderMetadata)
private val fileStack = mutableListOf<IrFile>()

override fun visitFile(declaration: IrFile): IrFile {
fileStack += declaration
return super.visitFile(declaration)
.also { fileStack.pop() }
}

override fun visitCall(expression: IrCall): IrExpression {
super.visitCall(expression)

val callee = expression.symbol.ensureBound(pluginContext.irProviders).owner

if (!callee.descriptor.annotations.hasAnnotation(InjektFqNames.ProviderOverload)) return expression

val memberScope = ((callee.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
?: (callee.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope())
?: error("Cannot get member scope for ${callee.descriptor}")

val overloadedFunction = memberScope.findFirstFunction(callee.name.asString()) { otherFunction ->
(otherFunction.extensionReceiverParameter
?: otherFunction.dispatchReceiverParameter)?.type == callee.extensionReceiverParameter?.type?.toKotlinType() &&
otherFunction.typeParameters.size == callee.typeParameters.size &&
otherFunction.valueParameters.size == callee.valueParameters.size &&
otherFunction.valueParameters.all { otherValueParameter ->
val calleeValueParameter =
callee.valueParameters[otherValueParameter.index]
if (otherValueParameter.type.constructor.declarationDescriptor == provider) {
calleeValueParameter.type.toKotlinType().constructor.declarationDescriptor == providerDefinition.classDescriptor
} else {
otherValueParameter.name == calleeValueParameter.name
}
}
}.let { symbolTable.referenceFunction(it).ensureBound(pluginContext.irProviders).owner }

return DeclarationIrBuilder(pluginContext, expression.symbol).run {
irCall(overloadedFunction).apply {
dispatchReceiver = expression.dispatchReceiver
extensionReceiver = expression.extensionReceiver

copyTypeArgumentsFrom(expression)

overloadedFunction.valueParameters.forEach { valueParameter ->
if (valueParameter.type.toKotlinType().constructor.declarationDescriptor == provider) {
putValueArgument(
valueParameter.index,
provider(
Name.identifier("provider_${expression.startOffset}"),
overloadedFunction.typeParameters
.associate { it.symbol to expression.getTypeArgument(it.index)!! },
expression.getValueArgument(valueParameter.index)!! as IrFunctionExpression
)
)
} else {
putValueArgument(
valueParameter.index,
expression.getValueArgument(valueParameter.index)
)
}
}
}
}
}

private fun IrBuilderWithScope.provider(
name: Name,
typeParameters: Map<IrTypeParameterSymbol, IrType>,
definition: IrFunctionExpression
): IrClass {
val definitionFunction = definition.function

val providerType = provider.defaultType.toIrType()
.substitute(typeParameters)

return irBlock {

}

return buildClass {
visibility = Visibilities.PUBLIC
this.name = name
origin = InjektDeclarationOrigin
}.apply clazz@{
superTypes = listOf(providerType)
createImplicitParameterDeclarationWithWrappedDescriptor()

val dependencies = mutableListOf<IrCall>()

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
})

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

valueParameters
.forEach { valueParameter ->
+irSetField(
irGet(thisReceiver!!),
fieldsByDependency.values.toList()[valueParameter.index],
irGet(valueParameter)
)
}
}
}

addFunction {
this.name = Name.identifier("invoke")
returnType = defaultType.arguments.single().typeOrNull!!
visibility = Visibilities.PUBLIC
}.apply {
dispatchReceiverParameter = thisReceiver?.copyTo(this)

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
})
}

annotations += providerMetadata(false)
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
 */