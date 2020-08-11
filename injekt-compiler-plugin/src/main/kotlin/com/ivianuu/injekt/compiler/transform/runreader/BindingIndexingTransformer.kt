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
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektIrContext
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

class BindingIndexingTransformer(
    private val indexer: Indexer,
    context: InjektIrContext
) : AbstractInjektTransformer(context) {

    override fun lower() {
        val declarations = mutableSetOf<Pair<IrDeclarationWithName, String>>()

        context.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given)) {
                    declarations += declaration.constructedClass to DeclarationGraph.BINDING_TAG
                }
                return super.visitConstructor(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (!declaration.isInEffect()) {
                    when {
                        declaration.hasAnnotation(InjektFqNames.Given) ->
                            declarations += declaration to DeclarationGraph.BINDING_TAG
                        declaration.hasAnnotation(InjektFqNames.MapEntries) ->
                            declarations += declaration to DeclarationGraph.MAP_ENTRIES_TAG
                        declaration.hasAnnotation(InjektFqNames.SetElements) ->
                            declarations += declaration to DeclarationGraph.SET_ELEMENTS_TAG
                    }
                }
                return super.visitFunction(declaration)
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                when {
                    declaration.hasAnnotation(InjektFqNames.Given) ->
                        declarations += declaration to DeclarationGraph.BINDING_TAG
                }
                return super.visitClass(declaration)
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) &&
                    !declaration.isInEffect()
                ) {
                    declarations += declaration to DeclarationGraph.BINDING_TAG
                }
                return super.visitProperty(declaration)
            }
        })

        declarations.forEach { indexer.index(it.first, it.second) }
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
