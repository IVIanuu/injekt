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

import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.ValueParameterRef
import com.ivianuu.injekt.compiler.generator.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias ComponentStatement = CodeBuilder.() -> Unit

interface ComponentMember {
    fun CodeBuilder.emit()
}

class ComponentCallable(
    val name: Name,
    val type: TypeRef,
    val isProperty: Boolean,
    val isSuspend: Boolean,
    val initializer: ComponentStatement?,
    val body: ComponentStatement?,
    val isMutable: Boolean,
    var isOverride: Boolean,
) : ComponentMember {
    override fun CodeBuilder.emit() {
        if (isOverride) emit("override ")
        if (isSuspend) emit("suspend ")
        if (isProperty) {
            if (isMutable) emit("var ") else emit("val ")
        } else {
            emit("fun ")
        }
        emit("$name")
        if (!isProperty) emit("()")
        emit(": ${type.render()}")
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

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val rawType: TypeRef
    abstract val owner: ComponentImpl
    abstract val origin: FqName?
    abstract val targetComponent: TypeRef?
    abstract val receiver: ComponentStatement?
}

class SelfGivenNode(
    override val type: TypeRef,
    val component: ComponentImpl,
) : GivenNode() {
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val rawType: TypeRef
        get() = type
    override val owner: ComponentImpl get() = component
    override val origin: FqName? get() = null
    override val targetComponent: TypeRef? get() = null
    override val receiver: ComponentStatement? get() = null
}

class ChildFactoryGivenNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val origin: FqName?,
    val childFactoryImpl: ComponentFactoryImpl,
) : GivenNode() {
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val rawType: TypeRef
        get() = type
    override val receiver: ComponentStatement?
        get() = null
    override val targetComponent: TypeRef?
        get() = owner.contextType
}

class CallableGivenNode(
    override val type: TypeRef,
    override val rawType: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    override val origin: FqName?,
    override val targetComponent: TypeRef?,
    override val receiver: ComponentStatement?,
    val valueParameters: List<ValueParameterRef>,
    val callable: Callable,
) : GivenNode() {
    override fun toString(): String = "Callable(${callable.fqName})"
}

class InputGivenNode(
    override val type: TypeRef,
    val name: String,
    override val owner: ComponentImpl,
) : GivenNode() {
    override val rawType: TypeRef
        get() = type
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val receiver: ComponentStatement?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
}

class MapGivenNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    val entries: List<CallableWithReceiver>,
) : GivenNode() {
    override val rawType: TypeRef
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val receiver: ComponentStatement?
        get() = null
}

class ProviderGivenNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    override val origin: FqName?,
) : GivenNode() {
    override val receiver: ComponentStatement?
        get() = null
    override val rawType: TypeRef
        get() = type
    override val targetComponent: TypeRef?
        get() = null
}

class SetGivenNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    val elements: List<CallableWithReceiver>,
) : GivenNode() {
    override val rawType: TypeRef
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
    override val receiver: ComponentStatement?
        get() = null
}

data class CallableWithReceiver(
    val callable: Callable,
    val receiver: ComponentStatement?,
)

class NullGivenNode(
    override val type: TypeRef,
    override val owner: ComponentImpl,
) : GivenNode() {
    override val rawType: TypeRef
        get() = type
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val receiver: ComponentStatement?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetComponent: TypeRef?
        get() = null
}

data class GivenRequest(val type: TypeRef, val origin: FqName)
