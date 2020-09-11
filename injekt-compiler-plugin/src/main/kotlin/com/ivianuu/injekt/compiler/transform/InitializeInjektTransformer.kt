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

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InitializeInjektTransformer(injektContext: InjektContext) :
    AbstractInjektTransformer(injektContext) {

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.initializeInjekt"
                ) {
                    val modules =
                        injektContext.moduleDescriptor.getPackage(InjektFqNames.IndexPackage)
                            .memberScope
                            .getContributedDescriptors()
                            .filterIsInstance<PropertyDescriptor>()
                            .map { FqName(it.name.asString().replace("_", ".")) }
                            .flatMap { injektContext.referenceFunctions(it) }
                            .map { it.owner }
                    return DeclarationIrBuilder(injektContext, expression.symbol).run {
                        irBlock {
                            modules.forEach { module ->
                                val targetContext = module.getAnnotation(InjektFqNames.Module)
                                    ?.getValueArgument(0)
                                    ?.let { it as IrClassReference }
                                    ?.classType
                                    ?: injektContext.injektSymbols.anyContext.defaultType
                                +irCall(
                                    injektContext.injektSymbols.moduleRegistry
                                        .owner
                                        .functions
                                        .single { it.name.asString() == "module" }
                                ).apply {
                                    dispatchReceiver =
                                        irGetObject(injektContext.injektSymbols.moduleRegistry)
                                    putValueArgument(
                                        0,
                                        irCall(
                                            injektContext.referenceFunctions(
                                                FqName("com.ivianuu.injekt.keyOf")
                                            ).single()
                                        ).apply {
                                            putTypeArgument(0, targetContext)
                                        }
                                    )
                                    putValueArgument(
                                        1,
                                        IrFunctionReferenceImpl(
                                            UNDEFINED_OFFSET,
                                            UNDEFINED_OFFSET,
                                            symbol.owner.valueParameters[1].type,
                                            module.symbol,
                                            0,
                                            1,
                                            null,
                                            null
                                        ).apply {
                                            if (symbol.owner.dispatchReceiverParameter != null) {
                                                dispatchReceiver =
                                                    irGetObject(symbol.owner.dispatchReceiverParameter!!.type.classOrNull!!)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                return super.visitCall(expression)
            }
        })
    }

}
