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

package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class IndexingTransformer(
    private val indexer: Indexer,
    injektContext: InjektContext
) : AbstractInjektTransformer(injektContext) {

    override fun lower() {
        // we have to defer the indexing because otherwise
        // we would get ConcurrentModficationExceptions
        val runnables = mutableListOf<() -> Unit>()

        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) ||
                    declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)
                ) {
                    runnables += {
                        indexer.index(
                            listOf(DeclarationGraph.GIVEN_PATH),
                            declaration.constructedClass
                        )
                    }
                }
                return super.visitConstructor(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (!declaration.isInModule()) {
                    when {
                        declaration.hasAnnotation(InjektFqNames.Given) ->
                            runnables += {
                                indexer.index(
                                    listOf(DeclarationGraph.GIVEN_PATH),
                                    declaration
                                )
                            }
                        declaration.hasAnnotation(InjektFqNames.GivenMapEntries) ->
                            runnables += {
                                indexer.index(
                                    listOf(
                                        DeclarationGraph.MAP_ENTRIES_PATH,
                                        declaration.returnType.uniqueTypeName().asString()
                                    ),
                                    declaration
                                )
                            }
                        declaration.hasAnnotation(InjektFqNames.GivenSetElements) ->
                            runnables += {
                                indexer.index(
                                    listOf(
                                        DeclarationGraph.SET_ELEMENTS_PATH,
                                        declaration.returnType.uniqueTypeName().asString()
                                    ),
                                    declaration
                                )
                            }
                    }
                }
                return super.visitFunction(declaration)
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                when {
                    declaration.hasAnnotation(InjektFqNames.Given) ||
                            declaration.hasAnnotatedAnnotations(InjektFqNames.Effect) ->
                        runnables += {
                            indexer.index(
                                listOf(DeclarationGraph.GIVEN_PATH),
                                declaration
                            )
                        }
                }
                return super.visitClass(declaration)
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) &&
                    !declaration.isInModule()
                ) {
                    runnables += {
                        indexer.index(
                            listOf(DeclarationGraph.GIVEN_PATH),
                            declaration
                        )
                    }
                }
                return super.visitProperty(declaration)
            }
        })

        runnables.forEach { it() }
    }

    private fun IrDeclaration.isInModule(): Boolean {
        var current: IrDeclaration? = parent as? IrDeclaration

        while (current != null) {
            if (current.hasAnnotation(InjektFqNames.Effect) ||
                current.hasAnnotation(InjektFqNames.GivenSet)
            ) return true
            current = current.parent as? IrDeclaration
        }

        return false
    }

}
