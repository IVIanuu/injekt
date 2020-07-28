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

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class WithInstancesTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            private val newDeclarations = mutableListOf<IrDeclaration>()

            override fun visitFileNew(declaration: IrFile): IrFile {
                return super.visitFileNew(declaration)
                    .also {
                        newDeclarations.forEach { declaration.addChild(it) }
                        newDeclarations.clear()
                    }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.withInstances"
                ) return result

                val component = buildClass {
                    name = nameProvider.allocateForGroup(
                        allScopes
                            .last {
                                val element = it.irElement
                                element is IrDeclarationWithVisibility &&
                                        element.visibility != Visibilities.LOCAL
                            }
                            .let {
                                "${it.scope.scopeOwner.name.asString()}WithInstances".asNameId()
                            }
                    )
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    parent = currentFile
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()
                    annotations += DeclarationIrBuilder(pluginContext, symbol)
                        .irCall(symbols.component.constructors.single())
                }

                newDeclarations += component

                return DeclarationIrBuilder(pluginContext, result.symbol).run {
                    irCall(
                        pluginContext.referenceFunctions(FqName("com.ivianuu.injekt.runReader"))
                            .single()
                    ).apply {
                        extensionReceiver = irCall(
                            pluginContext.referenceFunctions(FqName("com.ivianuu.injekt.childComponent"))
                                .single()
                        ).apply {
                            putTypeArgument(0, component.defaultType)
                            putValueArgument(
                                0,
                                result.getValueArgument(0)
                            )
                        }
                        putValueArgument(0, result.getValueArgument(1))
                    }
                }
            }
        })
    }

}