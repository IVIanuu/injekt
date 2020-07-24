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
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.hiddenDeprecatedAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope

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
                if ((declaration.hasAnnotation(InjektFqNames.Given) ||
                            declaration.hasAnnotation(InjektFqNames.MapEntries) ||
                            declaration.hasAnnotation(InjektFqNames.SetElements)) &&
                    !declaration.isInEffect()
                ) {
                    declarations += declaration
                }
                return super.visitFunction(declaration)
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) ||
                    declaration.hasAnnotation(InjektFqNames.RootComponentFactory) ||
                    declaration.hasAnnotation(InjektFqNames.EntryPoint)
                ) {
                    declarations += declaration
                }
                return super.visitClass(declaration)
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) &&
                    !declaration.isInEffect()
                ) {
                    declarations += declaration
                }
                return super.visitProperty(declaration)
            }
        })

        declarations.forEach { declaration ->
            val file = IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(
                    "<index for ${declaration.descriptor.fqNameSafe}>",
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
                    name = declaration.descriptor.fqNameSafe
                        .pathSegments().joinToString("_")
                        .asNameId()
                    kind = ClassKind.INTERFACE
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
                    annotations += DeclarationIrBuilder(pluginContext, symbol)
                        .hiddenDeprecatedAnnotation(pluginContext)
                }

                metadata = MetadataSource.File(
                    declarations.map { it.descriptor }
                )
            }

            module.files += file
            /*val file = File(outputDir, index.name.asString() + ".kt")
            file.parentFile.mkdirs()
            file.createNewFile()
            file.writeText(
                """
                package ${InjektFqNames.IndexPackage}
                
                import com.ivianuu.injekt.internal.Index
                
                @Index("${declaration.descriptor.fqNameSafe.asString()}")
                interface ${index.name}
            """.trimIndent()
            )
*/
            println("indexed ${file.render()}")
        }
    }

    private fun IrDeclaration.isInEffect(): Boolean {
        var current: IrDeclaration? = parent as? IrDeclaration

        while (current != null) {
            if (current.hasAnnotation(InjektFqNames.Effect)) return true
            current = current.parent as? IrDeclaration
        }

        return false
    }

}
