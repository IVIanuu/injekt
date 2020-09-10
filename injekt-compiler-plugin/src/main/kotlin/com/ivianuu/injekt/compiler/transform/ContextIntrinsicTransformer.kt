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

package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ContextIntrinsicTransformer(injektContext: InjektContext) :
    AbstractInjektTransformer(injektContext) {

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.<get-currentContext>") {
                    return expression.getValueArgument(0)
                        ?: error("Expected non-null context argument")
                } else if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader") {
                    return DeclarationIrBuilder(injektContext, expression.symbol)
                        .irCall(
                            injektContext.referenceFunctions(
                                FqName("com.ivianuu.injekt.runReaderDummy")
                            ).single()
                        ).apply {
                            copyTypeArgumentsFrom(expression)
                            putValueArgument(0, expression.extensionReceiver)
                            putValueArgument(1, expression.getValueArgument(0))
                            transformChildrenVoid()
                        }
                }
                return super.visitCall(expression)
            }
        })
    }

}
