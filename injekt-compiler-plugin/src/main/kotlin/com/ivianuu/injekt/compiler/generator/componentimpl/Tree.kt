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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.callableKind
import com.ivianuu.injekt.compiler.generator.getStarSubstitutionMap
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.renderExpanded
import com.ivianuu.injekt.compiler.generator.substitute
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias ComponentExpression = CodeBuilder.() -> Unit

interface ComponentMember {
    fun CodeBuilder.emit()
}

class ComponentCallable(
    val name: Name,
    val type: TypeRef,
    val isProperty: Boolean,
    val callableKind: Callable.CallableKind,
    val initializer: ComponentExpression?,
    val body: ComponentExpression?,
    val isMutable: Boolean,
    var isOverride: Boolean,
    val isInline: Boolean,
    val canBePrivate: Boolean,
    val valueParameters: List<ValueParameter>,
    val typeParameters: List<TypeParameter>
) : ComponentMember {
    override fun CodeBuilder.emit() {
        if (callableKind == Callable.CallableKind.COMPOSABLE) emitLine("@${InjektFqNames.Composable}")
        if (isOverride) emit("override ") else if (canBePrivate) emit("internal ")
        if (!isOverride && isInline) emit("inline ")
        if (callableKind == Callable.CallableKind.SUSPEND) emit("suspend ")
        if (isProperty) {
            if (isMutable) emit("var ") else emit("val ")
        } else {
            emit("fun ")
        }
        if (typeParameters.isNotEmpty()) {
            emit("<")
            typeParameters.forEachIndexed { index, typeParameter ->
                emit(typeParameter.name)
                if (index != typeParameters.lastIndex) emit(", ")
            }
            emit(">")
        }
        emit("$name")
        if (!isProperty) {
            emit("(")
            valueParameters.forEachIndexed { index, parameter ->
                emit("${parameter.name}: ${parameter.type.renderExpanded()}")
                if (index != valueParameters.lastIndex) emit(", ")
            }
            emit(")")
        }
        emit(": ${type.renderExpanded()}")
        if (typeParameters.isNotEmpty()) {
            emit(" where ")
            val typeParametersWithUpperBound = typeParameters
                .flatMap { typeParameter ->
                    typeParameter.upperBounds
                        .map { typeParameter to it }
                }
            typeParametersWithUpperBound.forEachIndexed { index, (typeParameter, upperBound) ->
                emit("${typeParameter.name} : ${upperBound.renderExpanded()}")
                if (index != typeParametersWithUpperBound.lastIndex) emit(", ")
            }
            emitSpace()
        }
        if (isProperty) {
            if (initializer != null) {
                emit(" = ")
                initializer!!()
            } else {
                emitLine()
                emit("    get() = ")
                body!!()
            }
        } else {
            emitSpace()
            braced {
                emit("return ")
                body!!()
            }
        }
    }

    data class ValueParameter(
        val name: Name,
        val type: TypeRef
    )

    data class TypeParameter(
        val name: Name,
        val upperBounds: List<TypeRef>
    )
}

sealed class BindingNode(
    type: TypeRef,
    var callableKind: Callable.CallableKind
) {
    abstract val dependencies: List<BindingRequest>
    abstract val rawType: TypeRef
    abstract val owner: ComponentImpl
    abstract val origin: FqName?
    abstract val targetComponent: TypeRef?
    abstract val declaredInComponent: ComponentImpl?
    abstract val receiver: ComponentExpression?
    abstract val isExternal: Boolean
    abstract val cacheable: Boolean
    abstract val inline: Boolean

    protected var _type = type
    val type: TypeRef by this::_type
    
    open fun refineType(dependencyBindings: List<BindingNode>) {
        dependencies.zip(dependencyBindings).forEach { (request, binding) ->
            val substitutionMap = binding.type.getStarSubstitutionMap(request.type)
            _type = _type.substitute(substitutionMap)
        }
    }
}

class SelfBindingNode(
    type: TypeRef,
    val component: ComponentImpl,
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val rawType: TypeRef
        get() = type
    override val owner: ComponentImpl get() = component
    override val origin: FqName? get() = null
    override val targetComponent: TypeRef? get() = null
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val receiver: ComponentExpression? get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inline: Boolean
        get() = true
}

class AssistedBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    val childComponent: ComponentImpl,
    val assistedTypes: List<TypeRef>
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val cacheable: Boolean
        get() = true
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val isExternal: Boolean
        get() = false
    override val origin: FqName?
        get() = null
    override val rawType: TypeRef
        get() = type
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val inline: Boolean
        get() = false

    override fun refineType(dependencyBindings: List<BindingNode>) {
        super.refineType(dependencyBindings)
        val returnType = type.typeArguments.last()
        val substitutionMap = childComponent.graph.getBinding(
            BindingRequest(returnType, FqName.ROOT, true, type.callableKind))
            .type.getStarSubstitutionMap(returnType)
        _type = _type.substitute(substitutionMap)
    }
}

class ChildComponentBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val origin: FqName?,
    val childComponent: ComponentImpl,
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val rawType: TypeRef
        get() = type
    override val receiver: ComponentExpression?
        get() = null
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = true
    override val inline: Boolean
        get() = false
}

class InputBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val declaredInComponent: ComponentImpl
        get() = owner
    override val cacheable: Boolean
        get() = false
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val isExternal: Boolean
        get() = false
    override val origin: FqName?
        get() = null
    override val rawType: TypeRef
        get() = type
    override val receiver: ComponentExpression?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val inline: Boolean
        get() = true
}

class CallableBindingNode(
    type: TypeRef,
    override val rawType: TypeRef,
    override val owner: ComponentImpl,
    override val declaredInComponent: ComponentImpl?,
    override val dependencies: List<BindingRequest>,
    override val receiver: ComponentExpression?,
    val callable: Callable
) : BindingNode(type, callable.callableKind) {
    override val isExternal: Boolean
        get() = callable.isExternal
    override val targetComponent: TypeRef?
        get() = callable.targetComponent
    override val origin: FqName?
        get() = callable.fqName
    override val cacheable: Boolean
        get() = callable.isFunBinding
    override val inline: Boolean
        get() = callable.visibility != Visibilities.PROTECTED &&
                (((!callable.isCall || callable.valueParameters.isEmpty()) ||
                        callable.isInline) &&!cacheable && targetComponent == null)

    override fun refineType(dependencyBindings: List<BindingNode>) {
        super.refineType(dependencyBindings)
        val substitutionMap = callable.type.getStarSubstitutionMap(type)
        _type = _type.substitute(substitutionMap)
    }

    override fun toString(): String = "Callable(${callable.type.render()})"
}

class MapBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    val entries: List<CallableWithReceiver>,
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val rawType: TypeRef
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inline: Boolean
        get() = false
}

class MissingBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val cacheable: Boolean
        get() = false
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val inline: Boolean
        get() = false
    override val isExternal: Boolean
        get() = false
    override val origin: FqName?
        get() = null
    override val rawType: TypeRef
        get() = type
    override val receiver: ComponentExpression?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
}

class ProviderBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    override val origin: FqName?,
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val rawType: TypeRef
        get() = type
    override val targetComponent: TypeRef?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = true
    override val inline: Boolean
        get() = false
}

class SetBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    val elements: List<CallableWithReceiver>,
) : BindingNode(type, Callable.CallableKind.DEFAULT) {
    override val rawType: TypeRef
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val declaredInComponent: ComponentImpl?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inline: Boolean
        get() = false
}

data class CallableWithReceiver(
    val callable: Callable,
    val receiver: ComponentExpression?,
    val declaredInComponent: ComponentImpl?,
    val substitutionMap: Map<ClassifierRef, TypeRef>
)

data class BindingRequest(
    val type: TypeRef,
    val origin: FqName,
    val required: Boolean,
    val callableKind: Callable.CallableKind
)
