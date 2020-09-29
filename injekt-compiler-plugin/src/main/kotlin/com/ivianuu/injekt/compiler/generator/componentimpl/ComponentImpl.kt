package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.Type
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.getAllCallables
import com.ivianuu.injekt.compiler.generator.render
import org.jetbrains.kotlin.name.Name

@Reader
class ComponentFactoryImpl(
    val name: Name,
    val factoryType: Type,
    val inputTypes: List<Type>,
    val contextType: Type,
    val parent: ComponentImpl?,
) : ComponentMember {

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
                emit("(")
                inputTypes.forEachIndexed { index, _ ->
                    emit("p$index")
                    if (index != inputTypes.lastIndex) emit(", ")
                }
                emit(")")
                emitLine()
            }
            with(context) { emit() }
        }
    }
}

@Reader
class ComponentImpl(
    val factoryImpl: ComponentFactoryImpl,
    val contextType: Type,
    val name: Name,
    val inputTypes: List<Type>,
) {

    val statements = GivenStatements(this)
    val graph = GivensGraph(this)

    val children = mutableListOf<ComponentFactoryImpl>()

    val members = mutableListOf<ComponentMember>()

    fun initialize() {
        val requests = contextType.getAllCallables()
        graph.checkRequests(requests.map { it.type })
        requests.forEach {
            statements.getFunction(
                type = it.type,
                name = it.name,
                isOverride = true,
                statement = statements.getGivenStatement(graph.resolvedGivens[it.type]!!)
            )
        }
    }

    fun CodeBuilder.emit() {
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

        emit(" : ${contextType.render()} ")
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
