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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.isNoArgProvider
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

interface Node

interface RequirementNode : Node {
    val key: Key
    val accessor: ComponentExpression
}

class ComponentRequirementNode(
    val component: ComponentImpl,
    override val key: Key,
    override val accessor: ComponentExpression
) : RequirementNode

class BindingRequest(
    val key: Key,
    val requestingKey: Key?,
    val requestOrigin: FqName?,
    val requestType: RequestType = key.inferRequestType()
) {

    fun copy(
        key: Key = this.key,
        requestingKey: Key? = this.requestingKey,
        requestOrigin: FqName? = this.requestOrigin,
        requestType: RequestType = this.requestType
    ): BindingRequest = BindingRequest(
        key, requestingKey, requestOrigin, requestType
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BindingRequest

        if (key != other.key) return false
        if (requestType != other.requestType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + requestType.hashCode()
        return result
    }

    override fun toString(): String {
        return "BindingRequest(key=$key, requestingKey=$requestingKey, requestOrigin=$requestOrigin, requestType=$requestType)"
    }

}

fun Key.inferRequestType() = when {
    type.isNoArgProvider() -> RequestType.Provider
    else -> RequestType.Instance
}

enum class RequestType {
    Instance,
    Provider
}

sealed class BindingNode(
    val key: Key,
    val context: IrClass?,
    val dependencies: List<BindingRequest>,
    val targetComponent: IrType?,
    val scoped: Boolean,
    val owner: ComponentImpl,
    val origin: FqName?
) : Node

class ChildComponentFactoryBindingNode(
    key: Key,
    owner: ComponentImpl,
    origin: FqName?,
    val parent: IrClass,
    val childComponentExpression: ComponentExpression
) : BindingNode(
    key, null, listOf(
        BindingRequest(
            parent.defaultType.asKey(),
            key,
            null
        )
    ),
    null, false, owner, origin
)

class ComponentImplBindingNode(
    val componentNode: ComponentRequirementNode,
) : BindingNode(
    componentNode.key,
    null,
    emptyList(),
    null,
    false,
    componentNode.component,
    componentNode.key.type.classOrNull!!.owner.fqNameForIrSerialization
)

class NullBindingNode(
    key: Key,
    owner: ComponentImpl
) : BindingNode(
    key,
    null,
    emptyList(),
    null,
    false,
    owner,
    null
)

class ProviderBindingNode(
    key: Key,
    owner: ComponentImpl,
    origin: FqName?
) : BindingNode(
    key,
    null,
    listOf(
        BindingRequest(
            key.type.typeArguments.single().typeOrFail.asKey(),
            key,
            origin
        )
    ),
    null,
    false,
    owner,
    origin
)

class ProvisionBindingNode(
    key: Key,
    context: IrClass?,
    dependencies: List<BindingRequest>,
    targetScope: IrType?,
    scoped: Boolean,
    owner: ComponentImpl,
    origin: FqName?,
    val createExpression: IrBuilderWithScope.(Map<InjektDeclarationIrBuilder.FactoryParameter, () -> IrExpression?>) -> IrExpression,
    val parameters: List<InjektDeclarationIrBuilder.FactoryParameter>
) : BindingNode(key, context, dependencies, targetScope, scoped, owner, origin)

fun IrType.asKey(): Key {
    return Key(this)
}

class Key(val type: IrType) {

    init {
        check(type !is IrErrorType) {
            "Cannot be error type ${type.render()}"
        }
        check(!type.isTypeParameter()) {
            "Must be concrete type ${type.render()}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Key
        if (type.distinctedType != other.type.distinctedType) return false
        return true
    }

    override fun hashCode(): Int = type.distinctedType
        .hashCode()

    override fun toString(): String = type.render()

    private val IrType.distinctedType: Any?
        get() = (type as? IrSimpleType)?.abbreviation
            ?.takeIf { it.typeAlias.owner.hasAnnotation(InjektFqNames.DistinctType) }
            ?: this

}
