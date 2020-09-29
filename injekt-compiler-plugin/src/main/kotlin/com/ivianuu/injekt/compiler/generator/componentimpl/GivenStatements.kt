package com.ivianuu.injekt.compiler.generator.componentimpl

/**
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.CallableRef
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.SimpleTypeRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Given
class GivenStatements(private val owner: ComponentImpl) {

private val parent = owner.factoryImpl.parent?.statements
private val statementsByType = mutableMapOf<TypeRef, ContextStatement>()

private fun getFunction(
type: TypeRef,
name: Name,
isOverride: Boolean,
statement: ContextStatement
): ContextFunction {
val existing = owner.members.firstOrNull {
it is ContextFunction && it.name == name
} as? ContextFunction
existing?.let {
if (isOverride) it.isOverride = true
return it
}
val function = ContextFunction(
name = name,
isOverride = isOverride,
type = type,
statement = statement
)
owner.members += function
return function
}

fun getGivenStatement(
given: GivenNode,
overriddenCallableType: TypeRef?
): ContextStatement {
statementsByType[given.type]?.let {
getFunction(
type = given.type,
name = overriddenCallableType?.uniqueTypeName() ?: given.type.uniqueTypeName(),
isOverride = overriddenCallableType != null,
statement = it
)
return it
}

val rawStatement = if (given.owner != owner) {
parent!!.getGivenStatement(given, null)
} else {
when (given) {
is CalleeContextGivenNode -> calleeContextExpression(given)
is ChildContextGivenNode -> childContextExpression(given)
is CallableGivenNode -> callableExpression(given)
is InputGivenNode -> inputExpression(given)
is MapGivenNode -> mapExpression(given)
is NullGivenNode -> nullExpression()
is SelfGivenNode -> selfContextExpression(given)
is SetGivenNode -> setExpression(given)
}
}

val finalStatement = if (given.targetContext == null ||
given.owner != owner
) rawStatement else ({
val property = ContextProperty(
name = given.type.uniqueTypeName(),
type = SimpleTypeRef(ClassifierRef(FqName("kotlin.Any")), isMarkedNullable = true),
initializer = { emit("this") },
isMutable = true
).also { owner.members += it }

emit("run ")
braced {
emitLine("var value = this@${owner.name}.${property.name}")
emitLine("if (value !== this@${owner.name}) return@run value as ${given.type.render()}")
emit("synchronized(this) ")
braced {
emitLine("value = this@${owner.name}.${property.name}")
emitLine("if (value !== this@${owner.name}) return@run value as ${given.type.render()}")
emit("value = ")
rawStatement()
emitLine()
emitLine("this@${owner.name}.${property.name} = value")
emitLine("return@run value as ${given.type.render()}")
}
}
})

val functionName =
overriddenCallableType?.uniqueTypeName() ?: given.type.uniqueTypeName()

getFunction(
type = given.type,
name = functionName,
isOverride = overriddenCallableType != null,
statement = finalStatement
)
if (overriddenCallableType != null &&
overriddenCallableType != given.type
) {
getFunction(
type = given.type,
name = given.type.uniqueTypeName(),
isOverride = false,
statement = finalStatement
)
}

val statement: ContextStatement = { emit("this@${owner.name}.${functionName}()") }

statementsByType[given.type] = statement

return statement
}

private fun childContextExpression(given: ChildContextGivenNode): ContextStatement =
{ emit("${given.childFactoryImpl.name}()") }

private fun calleeContextExpression(given: CalleeContextGivenNode): ContextStatement =
{ given.calleeContextStatement(this) }

private fun inputExpression(
given: InputGivenNode
): ContextStatement = { emit("this@${given.owner.name}.${given.name}") }

private fun mapExpression(given: MapGivenNode): ContextStatement {
return {
emit("run ")
braced {
emitLine("val result = mutableMapOf<Any?, Any?>()")
given.entries.forEach { (callable, receiver) ->
emit("result.putAll(")
emitCallableInvocation(callable, receiver, emptyList())
emitLine(")")
}
emitLine("result as ${given.type.render()}")
}
}
}

private fun setExpression(given: SetGivenNode): ContextStatement {
return {
emit("run ")
braced {
emitLine("val result = mutableSetOf<Any?>()")
given.elements.forEach { (callable, receiver) ->
emit("result.addAll(")
emitCallableInvocation(callable, receiver, emptyList())
emitLine(")")
}
emitLine("result as ${given.type.render()}")
}
}
}

private fun nullExpression(): ContextStatement = { emit("null") }

private fun callableExpression(given: CallableGivenNode): ContextStatement {
return {
if (given.callable.valueParameters.isNotEmpty()) {
emit("{ ")
given.callable.valueParameters.forEachIndexed { index, parameter ->
emit("p$index: ${parameter.typeRef.render()}")
if (index != given.callable.valueParameters.lastIndex) emit(", ")
}
emitLine(" ->")
emitCallableInvocation(
given.callable,
given.givenSetAccessStatement,
given.callable.valueParameters.mapIndexed { index, _ ->
{ emit("p$index") }
}
)
emitLine()
emitLine("}")
} else {
emitCallableInvocation(
given.callable,
given.givenSetAccessStatement,
emptyList()
)
}
}
}

private fun selfContextExpression(given: SelfGivenNode): ContextStatement =
{ emit("this@${given.component.name}") }

}

private fun CodeBuilder.emitCallableInvocation(
callable: CallableRef,
receiver: ContextStatement?,
parameters: List<ContextStatement>
) {
when {
receiver != null -> {
receiver()
emit(".${callable.name}")
}
callable.staticReceiver != null -> {
emit("${callable.staticReceiver.render()}.${callable.name}")
}
callable.valueParameters.any { it.isExtensionReceiver } -> {
parameters.first()()
emit(".${callable.name}")
}
else -> emit(callable.fqName)
}
if (!callable.isPropertyAccessor) {
emit("(")
parameters
.drop(if (callable.valueParameters.firstOrNull()?.isExtensionReceiver == true) 1 else 0)
.forEachIndexed { index, parameter ->
parameter()
if (index != parameters.lastIndex) emit(", ")
}
emit(")")
}
}
 */