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
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ImplicitIndexingTransformer(
    pluginContext: IrPluginContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    override fun lower() {
        val newDeclarations = mutableListOf<IrDeclaration>()

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration is IrSimpleFunction &&
                    declaration.isMarkedAsImplicit(pluginContext.bindingContext) &&
                    declaration.overriddenSymbols.isNotEmpty()
                ) {
                    newDeclarations += buildClass {
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
                }

                return super.visitFunctionNew(declaration)
            }
        })

        newDeclarations.forEach {
            it.file.addChild(it)
            indexer.index(it.descriptor.fqNameSafe)
        }
    }

}
