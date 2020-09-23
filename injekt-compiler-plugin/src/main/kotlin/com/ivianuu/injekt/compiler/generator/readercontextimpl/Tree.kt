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

package com.ivianuu.injekt.compiler.generator.readercontextimpl

import com.ivianuu.injekt.compiler.generator.CallableRef
import com.ivianuu.injekt.compiler.generator.CodeBuilder
import com.ivianuu.injekt.compiler.generator.ReaderContextDescriptor
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias ContextStatement = CodeBuilder.() -> Unit

interface ContextMember {
    fun CodeBuilder.emit()
}

class ContextFunction(
    val name: Name,
    val isOverride: Boolean,
    val type: TypeRef,
    val statement: ContextStatement
) : ContextMember {
    override fun CodeBuilder.emit() {
        if (isOverride) emit("override ")
        emit("fun $name(): ${type.render()} ")
        braced {
            emit("return ")
            statement()
            emitLine()
        }
    }
}

class ContextProperty(
    val name: Name,
    val type: TypeRef,
    val initializer: ContextStatement,
    val isMutable: Boolean
) : ContextMember {
    override fun CodeBuilder.emit() {
        if (isMutable) emit("var ") else emit("val ")
        emit("$name: ${type.render()} = ")
        initializer()
    }
}

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val owner: ContextImpl
    abstract val external: Boolean
    abstract val origin: FqName?
    abstract val targetContext: TypeRef?
    abstract val givenSetAccessStatement: ContextStatement?
    abstract val contexts: List<ReaderContextDescriptor>
}

class SelfGivenNode(
    override val type: TypeRef,
    val context: ContextImpl
) : GivenNode() {
    override val owner: ContextImpl get() = context
    override val external: Boolean get() = false
    override val origin: FqName? get() = null
    override val targetContext: TypeRef? get() = null
    override val givenSetAccessStatement: ContextStatement? get() = null
    override val contexts: List<ReaderContextDescriptor> get() = emptyList()
}

class ChildContextGivenNode(
    override val type: TypeRef,
    override val owner: ContextImpl,
    override val origin: FqName?,
    val childFactoryImpl: ContextFactoryImpl
) : GivenNode() {
    override val contexts: List<ReaderContextDescriptor>
        get() = emptyList()
    override val external: Boolean
        get() = false
    override val givenSetAccessStatement: ContextStatement?
        get() = null
    override val targetContext: TypeRef?
        get() = null// todo owner.contextId
}

class CalleeContextGivenNode(
    override val type: TypeRef,
    override val owner: ContextImpl,
    override val origin: FqName?,
    lazyCalleeContextStatement: () -> ContextStatement,
    private val lazyContexts: () -> List<ReaderContextDescriptor>
) : GivenNode() {
    val calleeContextStatement by unsafeLazy(lazyCalleeContextStatement)
    override val contexts by unsafeLazy {
        calleeContextStatement
        lazyContexts()
    }
    override val external: Boolean
        get() = false
    override val givenSetAccessStatement: ContextStatement?
        get() = null
    override val targetContext: TypeRef?
        get() = null
}

class CallableGivenNode(
    override val type: TypeRef,
    override val owner: ContextImpl,
    override val contexts: List<ReaderContextDescriptor>,
    override val origin: FqName?,
    override val external: Boolean,
    override val targetContext: TypeRef?,
    override val givenSetAccessStatement: ContextStatement?,
    val callable: CallableRef
) : GivenNode() {
    override fun toString(): String {
        return "Callable${callable.fqName}"
    }
}

class InputGivenNode(
    override val type: TypeRef,
    val name: String,
    override val owner: ContextImpl
) : GivenNode() {
    override val contexts: List<ReaderContextDescriptor>
        get() = emptyList()
    override val external: Boolean
        get() = false
    override val givenSetAccessStatement: ContextStatement?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetContext: TypeRef?
        get() = null
}

class MapGivenNode(
    override val type: TypeRef,
    override val owner: ContextImpl,
    override val contexts: List<ReaderContextDescriptor>,
    override val givenSetAccessStatement: ContextStatement?,
    val entries: List<CallableRef>
) : GivenNode() {
    override val external: Boolean
        get() = false
    override val origin: FqName?
        get() = null
    override val targetContext: TypeRef?
        get() = null
}

class SetGivenNode(
    override val type: TypeRef,
    override val owner: ContextImpl,
    override val contexts: List<ReaderContextDescriptor>,
    override val givenSetAccessStatement: ContextStatement?,
    val elements: List<CallableRef>
) : GivenNode() {
    override val external: Boolean
        get() = false
    override val origin: FqName?
        get() = null
    override val targetContext: TypeRef?
        get() = null
}

class NullGivenNode(
    override val type: TypeRef,
    override val owner: ContextImpl
) : GivenNode() {
    override val contexts: List<ReaderContextDescriptor>
        get() = emptyList()
    override val external: Boolean
        get() = false
    override val givenSetAccessStatement: ContextStatement?
        get() = null
    override val origin: FqName?
        get() = null
    override val targetContext: TypeRef?
        get() = null
}
