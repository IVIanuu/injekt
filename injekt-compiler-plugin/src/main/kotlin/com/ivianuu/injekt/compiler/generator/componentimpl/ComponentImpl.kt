package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.substitute
import org.jetbrains.kotlin.name.Name

@Binding
class ComponentImpl(
    @Assisted val componentType: TypeRef,
    @Assisted val name: Name,
    @Assisted val parent: ComponentImpl?,
    private val declarationStore: DeclarationStore,
    statementsFactory: (ComponentImpl) -> GivenStatements,
    graphFactory: (ComponentImpl) -> GivensGraph,
) : ComponentMember {

    val contextTreeNameProvider: UniqueNameProvider =
        parent?.contextTreeNameProvider ?: UniqueNameProvider()

    val statements = statementsFactory(this)
    val graph = graphFactory(this)

    val members = mutableListOf<ComponentMember>()

    private val componentConstructor = declarationStore.constructorForComponent(componentType)

    val constructorParameters = if (componentConstructor != null) {
        val substitutionMap = componentType.getSubstitutionMap(componentType.classifier.defaultType)
        componentConstructor
            .valueParameters
            .map { it.copy(type = it.type.substitute(substitutionMap)) }
    } else {
        emptyList()
    }

    fun initialize() {
        parent?.members?.add(this)
        val requests = declarationStore.allCallablesForType(componentType)
            .filter { it.givenKind == null }
        graph.checkRequests(requests.map { GivenRequest(it.type, it.fqName) })
        requests.forEach {
            statements.getCallable(
                type = it.type,
                name = it.name,
                isOverride = true,
                body = statements.getGivenStatement(graph.resolvedGivens[it.type]!!),
                isProperty = !it.isCall,
                isSuspend = it.isSuspend
            )
        }
    }

    override fun CodeBuilder.emit() {
        if (parent != null) emit("private inner ")
        emit("class $name")

        if (constructorParameters.isNotEmpty()) {
            emit("(")
            constructorParameters.forEachIndexed { index, param ->
                emit("${param.name}: ${param.type.render()}")
                if (index != constructorParameters.lastIndex) emit(", ")
            }
            emit(")")
        }

        emit(" : ${componentType.render()}")
        if (componentConstructor != null) {
            emit("(")
            constructorParameters.forEachIndexed { index, param ->
                emit(param.name)
                if (index != constructorParameters.lastIndex) emit(", ")
            }
            emit(") ")
        }
        braced {
            val renderedMembers = mutableSetOf<ComponentMember>()
            var currentMembers: List<ComponentMember> = members.toList()
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
