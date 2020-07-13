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

import com.ivianuu.injekt.compiler.compareTypeWithDistinct
import com.ivianuu.injekt.compiler.distinctedType
import com.ivianuu.injekt.compiler.hashWithDistinct
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class BindingNode(
    val key: Key,
    val dependencies: List<BindingRequest>,
    val targetComponent: IrType?,
    val scoped: Boolean,
    val owner: ComponentImpl,
    val origin: FqName?
)

class ChildComponentFactoryBindingNode(
    key: Key,
    owner: ComponentImpl,
    origin: FqName?,
    val parent: IrClass,
    val childComponentFactoryExpression: ComponentExpression
) : BindingNode(
    key, listOf(
        BindingRequest(
            parent.defaultType.asKey(),
            key,
            null
        )
    ),
    null, false, owner, origin
)

class ComponentImplBindingNode(
    val component: ComponentImpl,
) : BindingNode(
    component.factoryImpl.node.component.defaultType.asKey(),
    emptyList(),
    null,
    false,
    component,
    component.factoryImpl.node.component.descriptor.fqNameSafe
)

class GivenBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    targetComponent: IrType?,
    scoped: Boolean,
    owner: ComponentImpl,
    origin: FqName?,
    val createExpression: IrBuilderWithScope.(Map<BindingParameter, () -> IrExpression?>) -> IrExpression,
    val parameters: List<BindingParameter>
) : BindingNode(key, dependencies, targetComponent, scoped, owner, origin)

class InputParameterBindingNode(
    component: ComponentImpl,
    val inputParameter: IrValueParameter
) : BindingNode(
    inputParameter.type.asKey(),
    emptyList(),
    null,
    false,
    component,
    inputParameter.descriptor.fqNameSafe
)

class MapBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    owner: ComponentImpl,
    val functions: List<IrFunction>
) : BindingNode(
    key,
    dependencies,
    null,
    false,
    owner,
    null
)

class SetBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    owner: ComponentImpl,
    val functions: List<IrFunction>
) : BindingNode(
    key,
    dependencies,
    null,
    false,
    owner,
    null
)

class NullBindingNode(
    key: Key,
    owner: ComponentImpl
) : BindingNode(
    key,
    emptyList(),
    null,
    false,
    owner,
    null
)

class ProviderBindingNode(
    key: Key,
    owner: ComponentImpl,
    origin: FqName?,
) : BindingNode(
    key,
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

fun IrType.asKey(): Key = Key(this)

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
        return compareTypeWithDistinct(type, other.type)
    }

    override fun hashCode(): Int = type.hashWithDistinct()

    override fun toString(): String {
        return when (val distinctedType = type.distinctedType) {
            is IrTypeAliasSymbol -> distinctedType
            else -> type.render()
        }.toString()
    }

}

class BindingRequest(
    val key: Key,
    val requestingKey: Key?,
    val requestOrigin: FqName?
) {

    fun copy(
        key: Key = this.key,
        requestingKey: Key? = this.requestingKey,
        requestOrigin: FqName? = this.requestOrigin
    ): BindingRequest = BindingRequest(key, requestingKey, requestOrigin)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BindingRequest

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String =
        "BindingRequest(key=$key, requestingKey=$requestingKey, requestOrigin=$requestOrigin)"

}

data class BindingParameter(
    val name: String,
    val key: Key,
    val assisted: Boolean,
    val origin: FqName?
)
