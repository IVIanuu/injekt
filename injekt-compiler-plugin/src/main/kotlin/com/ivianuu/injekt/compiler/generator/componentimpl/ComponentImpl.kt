package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.getAllCallables
import com.ivianuu.injekt.compiler.generator.render
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.Name

@Given
class ComponentFactoryImpl(
    @Assisted val name: Name,
    @Assisted val factoryType: TypeRef,
    @Assisted val inputTypes: List<TypeRef>,
    @Assisted val contextType: TypeRef,
    @Assisted val parent: ComponentImpl?,
    /*componentFactory: (
        ComponentFactoryImpl,
        TypeRef,
        Name,
        List<TypeRef>
    ) -> ComponentImpl*/ // todo
    val module: ModuleDescriptor,
    statementsFactory: (ComponentImpl) -> GivenStatements,
    graphFactory: (ComponentImpl) -> GivensGraph,
) : ComponentMember {

    val contextTreeNameProvider: UniqueNameProvider =
        parent?.factoryImpl?.contextTreeNameProvider ?: UniqueNameProvider()

    val component = ComponentImpl(
        this,
        contextType,
        contextTreeNameProvider("C").asNameId(),
        inputTypes,
        module,
        statementsFactory,
        graphFactory
    )

    fun initialize() {
        parent?.members?.add(this)
        parent?.children?.add(this)
        component.initialize()
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
            emit("override fun invoke(")
            inputTypes.forEachIndexed { index, inputType ->
                emit("p$index: ${inputType.render()}")
                if (index != inputTypes.lastIndex) emit(", ")
            }
            emit("): ${contextType.render()} ")
            braced {
                emit("return ${component.name}")
                emit("(")
                inputTypes.forEachIndexed { index, _ ->
                    emit("p$index")
                    if (index != inputTypes.lastIndex) emit(", ")
                }
                emit(")")
                emitLine()
            }
            with(component) { emit() }
        }
    }
}

@Given
class ComponentImpl(
    @Assisted val factoryImpl: ComponentFactoryImpl,
    @Assisted val contextType: TypeRef,
    @Assisted val name: Name,
    @Assisted val inputTypes: List<TypeRef>,
    val module: ModuleDescriptor,
    statementsFactory: (ComponentImpl) -> GivenStatements,
    graphFactory: (ComponentImpl) -> GivensGraph,
) {

    val statements = statementsFactory(this)
    val graph = graphFactory(this)

    val children = mutableListOf<ComponentFactoryImpl>()

    val members = mutableListOf<ComponentMember>()

    fun initialize() {
        val requests = contextType.getAllCallables(module)
        graph.checkRequests(requests.map { GivenRequest(it.type, it.fqName) })
        requests.forEach {
            statements.getProperty(
                type = it.type,
                name = it.name,
                isOverride = true,
                getter = statements.getGivenStatement(graph.resolvedGivens[it.type]!!)
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
