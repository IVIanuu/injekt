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

package com.ivianuu.injekt.compiler.transform.runreader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ComponentIndexingTransformer(
    private val indexer: Indexer,
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
                    declaration.hasAnnotation(InjektFqNames.RunReaderContext)
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

        declarations.forEach { indexer.index(it) }
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
