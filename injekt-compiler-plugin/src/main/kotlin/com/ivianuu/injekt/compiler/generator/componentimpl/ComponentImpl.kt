/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.renderExpanded
import com.ivianuu.injekt.compiler.generator.substitute
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name

@Binding
class ComponentImpl(
    private val declarationStore: DeclarationStore,
    statementsFactory: (ComponentImpl) -> ComponentStatements,
    graphFactory: (ComponentImpl) -> BindingGraph,
    val componentType: @Assisted TypeRef,
    val name: @Assisted Name,
    val additionalInputTypes: @Assisted List<TypeRef>,
    val assistedRequests: @Assisted List<Callable>,
    val parent: @Assisted ComponentImpl?,
) {

    val isAssisted: Boolean
        get() = assistedRequests.isNotEmpty()

    val nonAssistedComponent: ComponentImpl
        get() = if (isAssisted) parent!!.nonAssistedComponent else this

    val rootComponent: ComponentImpl
        get() = parent?.rootComponent ?: this

    val contextTreeNameProvider: UniqueNameProvider =
        parent?.contextTreeNameProvider ?: UniqueNameProvider()

    val mergeDeclarations =
        declarationStore.mergeDeclarationsForMergeComponent(componentType.classifier.fqName)

    val children = mutableListOf<ComponentImpl>()
    val members = mutableListOf<ComponentMember>()

    val statements = statementsFactory(this)
    val graph = graphFactory(this)

    private val superComponentConstructor = declarationStore.constructorForComponent(componentType)

    private val superConstructorParameters = if (superComponentConstructor != null) {
        val substitutionMap = componentType.getSubstitutionMap(componentType.classifier.defaultType)
        superComponentConstructor
            .valueParameters
            .map { it.copy(type = it.type.substitute(substitutionMap)) }
    } else {
        emptyList()
    }

    val requests = (listOf(componentType) + mergeDeclarations
        .filterNot { it.isModule })
        .flatMap { declarationStore.allCallablesForType(it) }
        .filter { it.modality != Modality.FINAL } + assistedRequests

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        parent?.children?.add(this)
        graph.checkRequests(requests.map {
            BindingRequest(it.type, it.fqName, it.modality != Modality.OPEN, it.callableKind, false)
        })
        requests.forEach { requestCallable ->
            val binding = graph.resolvedBindings[requestCallable.type]!!
            if (binding is MissingBindingNode && requestCallable.modality == Modality.OPEN) return
            val body = if (binding is MissingBindingNode) ({
                emit("null")
            }) else statements.getBindingExpression(BindingRequest(
                requestCallable.type, requestCallable.fqName, true, requestCallable.callableKind, false))
            statements.getCallable(
                type = if (requestCallable in assistedRequests) binding.type else requestCallable.type,
                name = requestCallable.name,
                isOverride = requestCallable !in assistedRequests,
                body = body,
                isProperty = !requestCallable.isCall,
                callableKind = requestCallable.callableKind,
                cacheable = binding.cacheable,
                isInline = false,
                canBePrivate = false
            )
        }
    }

    fun CodeBuilder.emit() {
        if (parent != null) emit("private ")
        emit("class $name")

        val inputTypes = superConstructorParameters
            .map { it.type } + additionalInputTypes

        if (parent != null || inputTypes.isNotEmpty()) {
            emit("(")
            if (parent != null) {
                emit("val parent: ${parent.name}")
                if (inputTypes.isNotEmpty()) emit(", ")
            }
            inputTypes.forEachIndexed { index, input ->
                if (input in additionalInputTypes) emit("val ")
                emit("i_${input.uniqueTypeName()}: ${input.renderExpanded()}")
                if (index != inputTypes.lastIndex) emit(", ")
            }
            emit(")")
        }

        emit(" : ${componentType.renderExpanded()}")
        if (superComponentConstructor != null) {
            emit("(")
            superConstructorParameters.forEachIndexed { index, param ->
                emit("i_${param.type.uniqueTypeName()}")
                if (index != superConstructorParameters.lastIndex) emit(", ")
            }
            emit(") ")
        }

        val mergeSuperTypes = mergeDeclarations
            .filterNot { it.isModule }

        if (mergeSuperTypes.isNotEmpty()) {
            emit(", ")
            mergeSuperTypes.forEachIndexed { index, superType ->
                emit(superType.render())
                if (index != mergeSuperTypes.lastIndex) emit(", ")
            }
            emitSpace()
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
