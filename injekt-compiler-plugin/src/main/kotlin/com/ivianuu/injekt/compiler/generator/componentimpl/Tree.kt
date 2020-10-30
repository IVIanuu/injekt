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
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.renderExpanded
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
) : ComponentMember {
    override fun CodeBuilder.emit() {
        if (callableKind == Callable.CallableKind.COMPOSABLE) emitLine("@${InjektFqNames.Composable}")
        if (isOverride) emit("override ")
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

sealed class BindingNode {
    abstract val type: TypeRef
    abstract val dependencies: List<BindingRequest>
    abstract val rawType: TypeRef
    abstract val owner: ComponentImpl
    abstract val origin: FqName?
    abstract val targetComponent: TypeRef?
    abstract val receiver: ComponentExpression?
    abstract val isExternal: Boolean
    abstract val cacheable: Boolean
    abstract val callableKind: Callable.CallableKind
}

class SelfBindingNode(
    override val type: TypeRef,
    val component: ComponentImpl,
) : BindingNode() {
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
}

class ChildImplBindingNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val origin: FqName?,
    val childComponentImpl: ComponentImpl,
) : BindingNode() {
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
}

class CallableBindingNode(
    override val type: TypeRef,
    override val rawType: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    override val origin: FqName?,
    override val targetComponent: TypeRef?,
    override val receiver: ComponentExpression?,
    override val isExternal: Boolean,
    override val cacheable: Boolean,
    override val callableKind: Callable.CallableKind,
    val assistedParameters: List<TypeRef>,
    val callable: Callable
) : BindingNode() {
    override fun toString(): String = "Callable(${callable.type.render()})"
}

class DelegateBindingNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val callableKind: Callable.CallableKind,
    private val delegate: BindingRequest
) : BindingNode() {
    override val rawType: TypeRef
        get() = type
    override val targetComponent: TypeRef?
        get() = null
    override val receiver: ComponentExpression?
        get() = null
    override val cacheable: Boolean
        get() = false
    override val dependencies = listOf(delegate)
    override val isExternal: Boolean
        get() = false
    override val origin: FqName?
        get() = null
}

class MapBindingNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    val entries: List<CallableWithReceiver>,
) : BindingNode() {
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
}

class ProviderBindingNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    override val origin: FqName?,
) : BindingNode() {
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
}

class SetBindingNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<BindingRequest>,
    val elements: List<CallableWithReceiver>,
) : BindingNode() {
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
}

data class CallableWithReceiver(
    val callable: Callable,
    val receiver: ComponentExpression?,
    val substitutionMap: Map<ClassifierRef, TypeRef>
)

class NullBindingNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
) : BindingNode() {
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
}

data class BindingRequest(val type: TypeRef, val origin: FqName)
