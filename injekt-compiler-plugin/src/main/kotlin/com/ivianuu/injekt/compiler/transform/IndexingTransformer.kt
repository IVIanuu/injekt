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
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class IndexingTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        val declarations = mutableSetOf<IrDeclarationWithName>()

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given)) {
                    declarations += declaration.constructedClass
                }
                return super.visitConstructor(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given)) {
                    declarations += declaration
                }
                return super.visitFunction(declaration)
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given)) {
                    declarations += declaration
                }
                return super.visitClass(declaration)
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given)) {
                    declarations += declaration
                }
                return super.visitProperty(declaration)
            }
        })

        /*declarations.forEach { declaration ->
            val file = IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(
                    declaration.descriptor.fqNameSafe.pathSegments()
                        .joinToString("_") + ".kt",
                    intArrayOf()
                ),
                symbol = IrFileSymbolImpl(
                    object : PackageFragmentDescriptorImpl(
                        pluginContext.moduleDescriptor,
                        InjektFqNames.IndexPackage
                    ) {
                        override fun getMemberScope(): MemberScope = MemberScope.Empty
                    }
                ),
                InjektFqNames.IndexPackage
            ).apply {
                this.declarations += buildClass {
                    name = "${
                        declaration.descriptor.fqNameSafe
                            .pathSegments()
                            .joinToString("_")
                    }Index".asNameId()
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()
                    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                        irCall(symbols.index.constructors.single()).apply {
                            putValueArgument(
                                0,
                                irString(declaration.descriptor.fqNameSafe.asString())
                            )
                        }
                    }
                }

                metadata = MetadataSource.File(
                    this.declarations.map { it.descriptor }
                )
            }

            module.files += file
        }*/
    }

}
