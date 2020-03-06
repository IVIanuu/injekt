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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class TypedFunctionMarker(private val context: IrPluginContext) : IrElementTransformerVoid() {

    private val functionStack = mutableListOf<IrFunction>()
    private val typedCallingFunctions = mutableSetOf<IrFunction>()

    private val typed = context.moduleDescriptor.findClassAcrossModuleDependencies(
        ClassId.topLevel(InjektClassNames.Typed)
    )!!

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (!declaration.isInline ||
            declaration.typeParameters.none { it.isReified }) return declaration

        try {
            functionStack.push(declaration)
            super.visitFunction(declaration)

            if (declaration in typedCallingFunctions &&
                (!declaration.annotations.hasAnnotation(InjektClassNames.Typed) &&
                        !declaration.descriptor.annotations.hasAnnotation(InjektClassNames.Typed))) {
                declaration.annotations = declaration.annotations + IrConstructorCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.typeTranslator.translateType(typed.defaultType),
                    context.symbolTable.referenceConstructor(typed.unsubstitutedPrimaryConstructor!!),
                    0, 0, 1
                )
            }
        } finally {
            functionStack.pop()
        }

        return declaration
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val function = functionStack.lastOrNull() ?: return expression

        val descriptor = expression.symbol.descriptor

        if (descriptor.annotations.hasAnnotation(InjektClassNames.Typed) ||
            (expression.symbol.isBound && (expression.symbol.owner in typedCallingFunctions ||
                    expression.symbol.owner.annotations.hasAnnotation(InjektClassNames.Typed) ||
                    typedCallingFunctions.any { it.descriptor == expression.symbol.descriptor}))) {
            typedCallingFunctions += function
        }

        /*if (descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.typeOf" &&
                descriptor.isInline &&
                descriptor.valueParameters.isEmpty() &&
                descriptor.typeParameters.size == 1) {
            val exprTypeParameter = expression.getTypeArgument(0)!!

            if (function.typeParameters.any { it.defaultType == exprTypeParameter }) {
            } else {
                kotlin.error("lol ${expression.dump()}")
            }
        }*/

        return super.visitCall(expression)
    }

}