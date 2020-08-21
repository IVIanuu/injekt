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

package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class Given(
    val key: Key,
    val owner: IrClass,
    val contexts: List<IrClass>,
    val origin: FqName?,
    val external: Boolean,
    val targetContext: IrClass?,
    val givenSetAccessExpression: ContextExpression?
)

class GivenChildContext(
    key: Key,
    owner: IrClass,
    contexts: List<IrClass>,
    origin: FqName?,
    val factory: IrClass
) : Given(key, owner, contexts, origin, false, null, null)

class GivenFunction(
    key: Key,
    owner: IrClass,
    contexts: List<IrClass>,
    origin: FqName?,
    external: Boolean,
    targetContext: IrClass?,
    givenSetAccessExpression: ContextExpression?,
    val explicitParameters: List<IrValueParameter>,
    val function: IrFunction
) : Given(key, owner, contexts, origin, external, targetContext, givenSetAccessExpression)

class GivenInstance(
    val inputField: IrField,
    owner: IrClass
) : Given(
    inputField.type.asKey(),
    owner,
    emptyList(),
    inputField.descriptor.fqNameSafe,
    false,
    null,
    null
)

class GivenMap(
    key: Key,
    owner: IrClass,
    contexts: List<IrClass>,
    givenSetAccessExpression: ContextExpression?,
    val functions: List<IrFunction>
) : Given(
    key,
    owner,
    contexts,
    null,
    false,
    null,
    givenSetAccessExpression
)

class GivenSet(
    key: Key,
    owner: IrClass,
    contexts: List<IrClass>,
    givenSetAccessExpression: ContextExpression?,
    val functions: List<IrFunction>
) : Given(
    key,
    owner,
    contexts,
    null,
    false,
    null,
    givenSetAccessExpression
)

class GivenNull(
    key: Key,
    owner: IrClass
) : Given(
    key,
    owner,
    emptyList(),
    null,
    true,
    null,
    null
)

fun IrType.asKey(): Key =
    Key(this)

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
        return when (val distinctedType = type.typeOrTypeAlias) {
            is IrTypeAliasSymbol -> {
                buildString {
                    append(distinctedType.descriptor.fqNameSafe.asString())
                    if (type.typeArguments.isNotEmpty()) {
                        type.typeArguments.joinToString(
                            prefix = "<",
                            postfix = ">",
                            separator = ", "
                        ) {
                            it.render()
                        }
                    }
                    if (type.isMarkedNullable()) append("?")
                }
            }
            else -> type.render()
        }.toString()
    }

    private val IrType.typeOrTypeAlias: Any
        get() = (this as? IrSimpleType)?.abbreviation
            ?.typeAlias
            ?: this

    private fun compareTypeWithDistinct(
        a: IrType?,
        b: IrType?
    ): Boolean = a?.hashWithDistinct() == b?.hashWithDistinct()

    private fun IrType.hashWithDistinct(): Int {
        var result = 0
        val distinctedType = typeOrTypeAlias
        if (distinctedType is IrSimpleType) {
            result += 31 * distinctedType.classifier.hashCode()
            result += 31 * distinctedType.arguments.map { it.typeOrNull?.hashWithDistinct() ?: 0 }
                .hashCode()
        } else {
            result += 31 * distinctedType.hashCode()
        }

        val qualifier = getConstantFromAnnotationOrNull<String>(InjektFqNames.Qualifier, 0)
        if (qualifier != null) result += 31 * qualifier.hashCode()

        return result
    }

}

class GivenRequest(
    val key: Key,
    val requestOrigin: FqName?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GivenRequest

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String =
        "GivenRequest(key=$key, requestOrigin=$requestOrigin)"

}
