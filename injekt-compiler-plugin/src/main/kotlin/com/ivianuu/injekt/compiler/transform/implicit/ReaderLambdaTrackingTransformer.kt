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

import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext


// invocation observations
// set var
// set field
// putValueArgument

class ReaderLambdaTrackingTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun lower() {

    }

    /*override fun lower() {
        module.transformChildrenVoid(
            object : IrElementTransformerVoid() {
                override fun visitField(declaration: IrField): IrStatement {
                    println("visit field ${declaration.render()}\n" +
                            "is reader ${declaration.type.hasAnnotation(InjektFqNames.Reader)}")
                    return super.visitField(declaration)
                }

                override fun visitVariable(declaration: IrVariable): IrStatement {
                    println("visit var ${declaration.render()}\n" +
                            "is reader ${declaration.type.hasAnnotation(InjektFqNames.Reader)}")
                    return super.visitVariable(declaration)
                }

                override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
                    println("visit value parameter ${declaration.render()}\n" +
                            "is reader ${declaration.type.hasAnnotation(InjektFqNames.Reader)}")
                    return super.visitValueParameter(declaration)
                }
            }
        )
    }

    private fun readerLambda() = buildClass {
        name = nameProvider.allocateForGroup(
            "${declaration.descriptor.fqNameSafe.pathSegments()
                .joinToString("_")}ReaderImpl".asNameId()
        )
        kind = ClassKind.INTERFACE
        visibility = Visibilities.INTERNAL
    }.apply {
        parent = currentFile
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()
        annotations +=
            DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.readerImpl.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(
                            declaration.overriddenSymbols
                                .single()
                                .owner
                                .uniqueName()
                        )
                    )
                    putValueArgument(
                        1,
                        irString(declaration.uniqueName())
                    )
                }
            }
    }

    private fun transformReaderLambdaInvoke(call: IrCall): IrExpression {
        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            IrCallImpl(
                call.startOffset,
                call.endOffset,
                call.type,
                if (call.symbol.owner.isSuspend) {
                    pluginContext.tmpSuspendFunction(call.symbol.owner.valueParameters.size + 1)
                        .functions
                        .first { it.owner.name.asString() == "invoke" }
                } else {
                    pluginContext.tmpFunction(call.symbol.owner.valueParameters.size + 1)
                        .functions
                        .first { it.owner.name.asString() == "invoke" }
                }
            ).apply {
                copyTypeAndValueArgumentsFrom(call)
                putValueArgument(
                    valueArgumentsCount - 1,
                    currentReaderScope.contextExpression()
                )
            }
        }
    }*/

}
