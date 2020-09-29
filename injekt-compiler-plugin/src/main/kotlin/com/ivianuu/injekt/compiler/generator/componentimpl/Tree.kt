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
import com.ivianuu.injekt.compiler.generator.Type
import com.ivianuu.injekt.compiler.generator.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias ComponentStatement = CodeBuilder.() -> Unit

interface ComponentMember {
    fun CodeBuilder.emit()
}

class ComponentProperty(
    val name: Name,
    val type: Type,
    val initializer: ComponentStatement?,
    val getter: ComponentStatement?,
    val isMutable: Boolean,
    var isOverride: Boolean,
) : ComponentMember {
    override fun CodeBuilder.emit() {
        if (isOverride) emit("override ")
        if (isMutable) emit("var ") else emit("val ")
        emit("$name: ${type.render()}")
        if (initializer != null) {
            emit(" = ")
            initializer!!()
        } else {
            emitLine()
            emit("    get() = ")
            getter!!()
        }
    }
}

sealed class GivenNode {
    abstract val type: Type
    abstract val dependencies: List<GivenRequest>
    abstract val rawType: Type
    abstract val owner: ComponentImpl
    abstract val origin: FqName?
    abstract val targetComponent: Type?
    abstract val moduleAccessStatement: ComponentStatement?
}

class SelfGivenNode(
    override val type: Type,
    val component: ComponentImpl,
) : GivenNode() {
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val rawType: Type
        get() = type
    override val owner: ComponentImpl get() = component
    override val origin: FqName? get() = null
    override val targetComponent: Type? get() = null
    override val moduleAccessStatement: ComponentStatement? get() = null
}

class ChildFactoryGivenNode(
    override val type: Type,
    override val owner: ComponentImpl,
    override val origin: FqName?,
    val childFactoryImpl: ComponentFactoryImpl,
) : GivenNode() {
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val rawType: Type
        get() = type
    override val moduleAccessStatement: ComponentStatement?
        get() = null
    override val targetComponent: Type?
        get() = owner.contextType
}

class CallableGivenNode(
    override val type: Type,
    override val rawType: Type,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    override val origin: FqName?,
    override val targetComponent: Type?,
    override val moduleAccessStatement: ComponentStatement?,
    val callable: Callable,
) : GivenNode() {
    override fun toString(): String = "Callable${callable.fqName}"
}

class InputGivenNode(
    override val type: Type,
    val name: String,
    override val owner: ComponentImpl,
) : GivenNode() {
    override val rawType: Type
        get() = type
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val moduleAccessStatement: ComponentStatement?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetComponent: Type?
        get() = null
}

class MapGivenNode(
    override val type: Type,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    val entries: List<CallableWithReceiver>,
) : GivenNode() {
    override val rawType: Type
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: Type?
        get() = null
    override val moduleAccessStatement: ComponentStatement?
        get() = null
}

class SetGivenNode(
    override val type: Type,
    override val owner: ComponentImpl,
    override val dependencies: List<GivenRequest>,
    val elements: List<CallableWithReceiver>,
) : GivenNode() {
    override val rawType: Type
        get() = type
    override val origin: FqName?
        get() = null
    override val targetComponent: Type?
        get() = null
    override val moduleAccessStatement: ComponentStatement?
        get() = null
}

data class CallableWithReceiver(
    val callable: Callable,
    val receiver: ComponentStatement?,
)

class NullGivenNode(
    override val type: Type,
    override val owner: ComponentImpl,
) : GivenNode() {
    override val rawType: Type
        get() = type
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val moduleAccessStatement: ComponentStatement?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetComponent: Type?
        get() = null
}

data class GivenRequest(val type: Type, val origin: FqName)
