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

package com.ivianuu.injekt.compiler.transform.runreader

import com.ivianuu.injekt.compiler.compareTypeWithDistinct
import com.ivianuu.injekt.compiler.distinctedType
import com.ivianuu.injekt.compiler.hashWithDistinct
import com.ivianuu.injekt.compiler.isTypeParameter
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class BindingNode(
    val key: Key,
    val contexts: List<IrClass>,
    val origin: FqName?
)

class GivenBindingNode(
    key: Key,
    contexts: List<IrClass>,
    origin: FqName?,
    val createExpression: IrBuilderWithScope.(Map<IrValueParameter, () -> IrExpression?>, () -> IrExpression) -> IrExpression,
    val explicitParameters: List<IrValueParameter>,
    val function: IrFunction
) : BindingNode(key, contexts, origin)

class InputBindingNode(val inputField: IrField) : BindingNode(
    inputField.type.asKey(),
    emptyList(),
    inputField.descriptor.fqNameSafe
)

class MapBindingNode(
    key: Key,
    contexts: List<IrClass>,
    val functions: List<IrFunction>
) : BindingNode(
    key,
    contexts,
    null
)

class SetBindingNode(
    key: Key,
    contexts: List<IrClass>,
    val functions: List<IrFunction>
) : BindingNode(
    key,
    contexts,
    null
)

class NullBindingNode(key: Key) : BindingNode(
    key,
    emptyList(),
    null
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
