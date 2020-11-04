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
import com.ivianuu.injekt.compiler.generator.getStarSubstitutionMap
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.renderExpanded
import com.ivianuu.injekt.compiler.generator.substitute
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
    val canBePrivate: Boolean
) : ComponentMember {
    override fun CodeBuilder.emit() {
        if (callableKind == Callable.CallableKind.COMPOSABLE) emitLine("@${InjektFqNames.Composable}")
        if (isOverride) emit("override ") else if (canBePrivate) emit("private ")
        if (!isOverride && isInline) emit("inline ")
        if (callableKind == Callable.CallableKind.SUSPEND) emit("suspend ")
        if (isProperty) {
            if (isMutable) emit("var ") else emit("val ")
        } else {
            emit("fun ")
        }
        emit("$name")
        if (!isProperty) emit("()")
        emit(": ${type.renderExpanded()}")
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
}

sealed class BindingNode(
    type: TypeRef
) {
    abstract val dependencies: List<BindingRequest>
    abstract val rawType: TypeRef
    abstract val owner: ComponentImpl
    abstract val origin: FqName?
    abstract val targetComponent: TypeRef?
    abstract val receiver: ComponentExpression?
    abstract val isExternal: Boolean
    abstract val cacheable: Boolean
    abstract val callableKind: Callable.CallableKind
    abstract val inlineMode: InlineMode

    protected var _type = type
    val type: TypeRef by this::_type
    
    open fun refineType(dependencyBindings: List<BindingNode>) {
        dependencies.zip(dependencyBindings).forEach { (request, binding) ->
            val substitutionMap = binding.type.getStarSubstitutionMap(request.type)
            _type = _type.substitute(substitutionMap)
        }
    }
    
    enum class InlineMode {
        NONE, FUNCTION, EXPRESSION
    }
}

class SelfBindingNode(
    type: TypeRef,
    val component: ComponentImpl,
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val rawType: TypeRef
        get() = type
    override val owner: ComponentImpl get() = component
    override val origin: FqName? get() = null
    override val targetComponent: TypeRef? get() = null
    override val receiver: ComponentExpression? get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inlineMode: InlineMode
        get() = InlineMode.EXPRESSION
}

class AssistedBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    val childComponent: ComponentImpl,
    val assistedTypes: List<TypeRef>
) : BindingNode(type) {
    override val cacheable: Boolean
        get() = true
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
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
    override val inlineMode: InlineMode
        get() = InlineMode.NONE

    override fun refineType(dependencyBindings: List<BindingNode>) {
        super.refineType(dependencyBindings)
        val returnType = type.typeArguments.last()
        val substitutionMap = childComponent.graph.getBinding(BindingRequest(returnType, FqName.ROOT))
            .type.getStarSubstitutionMap(returnType)
        _type = _type.substitute(substitutionMap)
    }
}

class ChildComponentBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val origin: FqName?,
    val childComponent: ComponentImpl,
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val rawType: TypeRef
        get() = type
    override val receiver: ComponentExpression?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = true
    override val inlineMode: InlineMode
        get() = InlineMode.NONE
}

class InputBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl
) : BindingNode(type) {
    override val cacheable: Boolean
        get() = false
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
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
    override val inlineMode: InlineMode
        get() = InlineMode.EXPRESSION
}

class CallableBindingNode(
    type: TypeRef,
    override val rawType: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    override val receiver: ComponentExpression?,
    val callable: Callable
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = callable.callableKind
    override val isExternal: Boolean
        get() = callable.isExternal
    override val targetComponent: TypeRef?
        get() = callable.targetComponent
    override val origin: FqName?
        get() = callable.fqName
    override val cacheable: Boolean
        get() = callable.isEager
    override val inlineMode: InlineMode = when {
        ((!callable.isCall || callable.valueParameters.isEmpty()) ||
                callable.isInline) &&
                !cacheable &&
                targetComponent == null -> InlineMode.FUNCTION
        else -> InlineMode.NONE
    }

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
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
    override val rawType: TypeRef
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inlineMode: InlineMode
        get() = InlineMode.NONE
}

class ProviderBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    override val origin: FqName?,
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
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
    override val inlineMode: InlineMode
        get() = InlineMode.NONE
}

class SetBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    val elements: List<CallableWithReceiver>,
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
    override val rawType: TypeRef
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inlineMode: InlineMode
        get() = InlineMode.NONE
}

data class CallableWithReceiver(
    val callable: Callable,
    val receiver: ComponentExpression?,
    val substitutionMap: Map<ClassifierRef, TypeRef>
)

class NullBindingNode(
    type: TypeRef,
    override val owner: ComponentImpl,
) : BindingNode(type) {
    override val callableKind: Callable.CallableKind
        get() = Callable.CallableKind.DEFAULT
    override val rawType: TypeRef
        get() = type
    override val dependencies: List<BindingRequest>
        get() = emptyList()
    override val receiver: ComponentExpression?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val isExternal: Boolean
        get() = false
    override val cacheable: Boolean
        get() = false
    override val inlineMode: InlineMode
        get() = InlineMode.EXPRESSION
}

data class BindingRequest(val type: TypeRef, val origin: FqName)
