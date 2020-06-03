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

import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType

sealed class Path {
    abstract fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall
}

class ClassPath(val clazz: IrClass) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astClassPath.constructors.single()).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    builder.context.irBuiltIns.kClassClass.typeWith(clazz.defaultType),
                    clazz.symbol,
                    clazz.defaultType
                )
            )
        }

    }
}

class PropertyPath(val property: InjektDeclarationIrBuilder.FieldWithGetter) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astPropertyPath.constructors.single()).apply {
            putValueArgument(0, builder.irString(property.field.name.asString()))
        }
    }
}

class TypeParameterPath(val typeParameter: IrTypeParameter) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astTypeParameterPath.constructors.single()).apply {
            putValueArgument(0, builder.irString(typeParameter.name.asString()))
        }
    }
}
