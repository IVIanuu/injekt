package com.ivianuu.injekt.compiler.generator.componentimpl

/**
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.name.Name

@Reader
class ComponentFactoryImpl(
val name: Name,
val factoryType: TypeRef,
val inputTypes: List<TypeRef>,
val contextType: TypeRef,
val parent: ComponentImpl?
) : ContextMember {

val contextTreeNameProvider: UniqueNameProvider =
parent?.factoryImpl?.contextTreeNameProvider ?: UniqueNameProvider()

val context = ComponentImpl(
this,
contextType,
contextTreeNameProvider("C").asNameId(),
inputTypes
)

fun initialize() {
parent?.members?.add(this)
parent?.children?.add(this)
context.initialize()
}

override fun CodeBuilder.emit() {
if (parent == null) {
emit("object ")
} else {
emit("private inner class ")
}
emit(name)
emit(" : ${factoryType.render()} ")
braced {
emit("override fun create(")
inputTypes.forEachIndexed { index, inputType ->
emit("p$index: ${inputType.render()}")
if (index != inputTypes.lastIndex) emit(", ")
}
emit("): ${contextType.render()} ")
braced {
emit("return ${context.name}")
//if (inputTypes.isNotEmpty()) {
emit("(")
inputTypes.forEachIndexed { index, _ ->
emit("p$index")
if (index != inputTypes.lastIndex) emit(", ")
}
emit(")")
//}
emitLine()
}
with(context) { emit() }
}
}
}

@Reader
class ComponentImpl(
val factoryImpl: ComponentFactoryImpl,
val contextId: TypeRef,
val name: Name,
val inputTypes: List<TypeRef>
) {

val statements = GivenStatements(this)
val graph = GivensGraph(this)

val children = mutableListOf<ComponentFactoryImpl>()

val members = mutableListOf<ContextMember>()

val superTypes = mutableSetOf<TypeRef>()

fun initialize() {
val declarationStore = given<DeclarationStore>()
val entryPoints = declarationStore.getRunReaderContexts(contextId.classifier.fqName)
.map { declarationStore.getReaderContextByFqName(it)!! }
.map { entryPoint ->
// this is really naive and probably error prone
entryPoint.copy(
type =
if (factoryImpl.factoryType.classifier.typeParameters.size ==
entryPoint.type.classifier.typeParameters.size &&
factoryImpl.factoryType.classifier.typeParameters.zip(
entryPoint.type.classifier.typeParameters
).all { it.first.fqName.shortName() == it.second.fqName.shortName() }
) {
entryPoint.type.typeWith(factoryImpl.factoryType.typeArguments.map { it })
} else entryPoint.type
)
}

graph.checkEntryPoints(entryPoints)
}

fun CodeBuilder.emit() {
emitLine("@com.ivianuu.injekt.internal.ContextImplMarker")
emit("private ")
if (factoryImpl.parent != null) emit("inner ")
emit("class $name")
if (inputTypes.isNotEmpty()) {
emit("(")
inputTypes.forEachIndexed { index, inputType ->
emit("private val p$index: ${inputType.render()}")
if (index != inputTypes.lastIndex) emit(", ")
}
emit(")")
}

emit(" : ${contextId.render()}")
if (superTypes.isNotEmpty()) {
emit(", ")
superTypes.forEachIndexed { index, superType ->
emit(superType.render())
if (index != superTypes.size - 1) emit(", ")
}
}
emitSpace()
braced {
val renderedMembers = mutableSetOf<ContextMember>()
var currentMembers: List<ContextMember> = members.toList()
while (currentMembers.isNotEmpty()) {
renderedMembers += currentMembers
currentMembers.forEach {
with(it) { emit() }
emitLine()
}
currentMembers = members.filterNot { it in renderedMembers }
}
}
}
}
 */