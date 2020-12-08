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

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.defaultType
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias ComponentType = TypeRef
typealias ScopeType = TypeRef
typealias ComponentFactoryType = TypeRef

@Binding class ComponentImpl(
    private val declarationStore: DeclarationStore,
    membersFactory: (ComponentImpl) -> ComponentMembers,
    graphFactory: (ComponentImpl) -> BindingGraph,
    val componentType: ComponentType?,
    val request: Callable?,
    val scopeType: ScopeType?,
    val componentFactoryType: ComponentFactoryType?,
    val name: Name,
    val inputTypes: List<TypeRef>,
    val isAssisted: Boolean,
    val parent: @Parent ComponentImpl?,
) {

    val type = ClassifierRef(FqName.topLevel(name)).defaultType

    val nonAssistedComponent: ComponentImpl
        get() = if (isAssisted) parent!!.nonAssistedComponent else this

    val rootComponent: ComponentImpl
        get() = parent?.rootComponent ?: this

    val contextTreeNameProvider: UniqueNameProvider =
        parent?.contextTreeNameProvider ?: UniqueNameProvider()

    val mergeDeclarations = if (componentType != null)
        declarationStore.mergeDeclarationsForMergeComponent(componentType.classifier.fqName)
    else emptyList()

    val children = mutableListOf<ComponentImpl>()
    val members = mutableListOf<ComponentMember>()

    val statements = membersFactory(this)
    val graph = graphFactory(this)

    val requests = (componentType?.let {
        declarationStore.allCallablesForType(componentType)
            .filter { it.modality != Modality.FINAL }
    } ?: listOfNotNull(request)) + mergeDeclarations
        .filterNot { it.isModule }
        .flatMap { declarationStore.allCallablesForType(it) }
        .filter { it.modality != Modality.FINAL }

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        parent?.children?.add(this)
        graph.checkRequests(
            requests.map {
                BindingRequest(it.type,
                    it.fqName,
                    it.modality != Modality.OPEN,
                    it.callableKind,
                    false,
                    false
                )
            }
        )
        requests.forEach { requestCallable ->
            val binding = graph.resolvedBindings[requestCallable.type]!!
            if (binding is MissingBindingNode && requestCallable.modality == Modality.OPEN) return
            val body = if (binding is MissingBindingNode) ({
                emit("null")
            }) else statements.getBindingExpression(BindingRequest(
                requestCallable.type,
                requestCallable.fqName,
                true,
                requestCallable.callableKind,
                false,
                false)
            )
            statements.getCallable(
                type = requestCallable.type,
                name = requestCallable.name,
                isOverride = componentType != null,
                body = body,
                isProperty = !requestCallable.isCall,
                callableKind = requestCallable.callableKind,
                eager = binding.eager,
                isInline = componentType == null
            )
        }
    }

    fun CodeBuilder.emit() {
        if (parent != null) emit("private ")
        emit("class $name")

        if (parent != null || inputTypes.isNotEmpty()) {
            emit("(")
            if (parent != null) {
                emit("internal val _parent: ${parent.name}")
                if (inputTypes.isNotEmpty()) emit(", ")
            }
            inputTypes.forEachIndexed { index, input ->
                if (input in this@ComponentImpl.inputTypes) emit("internal val ")
                emit("i_${input.uniqueTypeName()}: ${input.render(expanded = true)}")
                if (index != inputTypes.lastIndex) emit(", ")
            }
            emit(")")
        }

        if (componentType != null) {
            emit(" : ${componentType.render(expanded = true)}")
            val mergeSuperTypes = mergeDeclarations
                .filterNot { it.isModule }

            if (mergeSuperTypes.isNotEmpty()) {
                emit(", ")
                mergeSuperTypes.forEachIndexed { index, superType ->
                    emit(superType.render())
                    if (index != mergeSuperTypes.lastIndex) emit(", ")
                }
            }
        }

        emitSpace()

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
