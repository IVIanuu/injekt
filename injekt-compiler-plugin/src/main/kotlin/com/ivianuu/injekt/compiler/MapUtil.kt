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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

val SupportedMapKeyTypes: List<FqName> = listOf(
    KotlinBuiltIns.FQ_NAMES.kClass.toSafe(),
    KotlinBuiltIns.FQ_NAMES._int.toSafe(),
    KotlinBuiltIns.FQ_NAMES._long.toSafe(),
    KotlinBuiltIns.FQ_NAMES.string.toSafe()
)

sealed class MapKey {
    abstract fun IrBuilderWithScope.asExpression(): IrExpression
}

data class ClassKey(val value: IrType) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression {
        return IrClassReferenceImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.kClassClass.typeWith(value),
            value.classifierOrFail,
            value
        )
    }

    override fun toString(): String = "kotlin.reflect.KClass<${value.render()}>"
}

data class IntKey(val value: Int) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression = irInt(value)
    override fun toString(): String = value.toString()
}

data class LongKey(val value: Long) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression = irLong(value)
    override fun toString(): String = value.toString()
}

data class StringKey(val value: String) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression = irString(value)
    override fun toString(): String = value
}
