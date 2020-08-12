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
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GlobalBindingIndexingTransformer(
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
                            DeclarationGraph.GIVEN_TAG,
                            declaration.constructedClass.defaultType.uniqueTypeName().asString(),
                            declaration.constructedClass
                        )
                        indexer.index(
                            declaration,
                            DeclarationGraph.GIVEN_CONTEXTS,
                            declaration.getContext()!!.descriptor.fqNameSafe.asString(),
                        ) {
                            annotations += DeclarationIrBuilder(
                                injektContext,
                                declaration.symbol
                            ).run {
                                irCall(injektContext.injektSymbols.givenContext.constructors.single()).apply {
                                    putValueArgument(
                                        0,
                                        irString(
                                            declaration.constructedClass.defaultType.uniqueTypeName()
                                                .asString()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                return super.visitConstructor(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (!declaration.isInModule()) {
                    when {
                        declaration.hasAnnotation(InjektFqNames.Given) ||
                                declaration.hasAnnotatedAnnotations(InjektFqNames.Effect) ->
                            runnables += {
                                val explicitParameters = declaration.valueParameters
                                    .filter { it != declaration.getContextValueParameter() }
                                val key =
                                    if (explicitParameters.isEmpty()) declaration.returnType.uniqueTypeName()
                                        .asString()
                                    else injektContext.tmpFunction(explicitParameters.size)
                                        .typeWith(explicitParameters.map { it.type } + declaration.returnType)
                                        .uniqueTypeName()
                                        .asString()
                                indexer.index(
                                    DeclarationGraph.GIVEN_TAG,
                                    key,
                                    declaration
                                )
                                indexer.index(
                                    declaration,
                                    DeclarationGraph.GIVEN_CONTEXTS,
                                    declaration.getContext()!!.descriptor.fqNameSafe.asString(),
                                ) {
                                    annotations += DeclarationIrBuilder(
                                        injektContext,
                                        declaration.symbol
                                    ).run {
                                        irCall(injektContext.injektSymbols.givenContext.constructors.single()).apply {
                                            putValueArgument(
                                                0,
                                                irString(key)
                                            )
                                        }
                                    }
                                }
                            }
                        declaration.hasAnnotation(InjektFqNames.MapEntries) ->
                            runnables += {
                                indexer.index(
                                    DeclarationGraph.MAP_ENTRIES_TAG,
                                    declaration.returnType.uniqueTypeName().asString(),
                                    declaration
                                )
                                indexer.index(
                                    declaration,
                                    DeclarationGraph.GIVEN_CONTEXTS,
                                    declaration.getContext()!!.descriptor.fqNameSafe.asString(),
                                ) {
                                    annotations += DeclarationIrBuilder(
                                        injektContext,
                                        declaration.symbol
                                    ).run {
                                        irCall(injektContext.injektSymbols.givenContext.constructors.single()).apply {
                                            putValueArgument(
                                                0,
                                                irString(
                                                    declaration.returnType.uniqueTypeName()
                                                        .asString()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        declaration.hasAnnotation(InjektFqNames.SetElements) ->
                            runnables += {
                                indexer.index(
                                    DeclarationGraph.SET_ELEMENTS_TAG,
                                    declaration.returnType.uniqueTypeName().asString(),
                                    declaration
                                )
                                indexer.index(
                                    declaration,
                                    DeclarationGraph.GIVEN_CONTEXTS,
                                    declaration.getContext()!!.descriptor.fqNameSafe.asString(),
                                ) {
                                    annotations += DeclarationIrBuilder(
                                        injektContext,
                                        declaration.symbol
                                    ).run {
                                        irCall(injektContext.injektSymbols.givenContext.constructors.single()).apply {
                                            putValueArgument(
                                                0,
                                                irString(
                                                    declaration.returnType.uniqueTypeName()
                                                        .asString()
                                                )
                                            )
                                        }
                                    }
                                }
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
                            val readerConstructor =
                                declaration.getReaderConstructor(injektContext)!!
                            val explicitParameters = readerConstructor.valueParameters
                                .filter { it != readerConstructor.getContextValueParameter() }
                            val key =
                                if (explicitParameters.isEmpty()) readerConstructor.returnType.uniqueTypeName()
                                    .asString()
                                else injektContext.tmpFunction(explicitParameters.size)
                                    .typeWith(explicitParameters.map { it.type } + readerConstructor.returnType)
                                    .uniqueTypeName()
                                    .asString()
                            indexer.index(
                                DeclarationGraph.GIVEN_TAG,
                                key,
                                declaration
                            )
                            indexer.index(
                                declaration,
                                DeclarationGraph.GIVEN_CONTEXTS,
                                readerConstructor.getContext()!!.descriptor.fqNameSafe.asString(),
                            ) {
                                annotations += DeclarationIrBuilder(
                                    injektContext,
                                    declaration.symbol
                                ).run {
                                    irCall(injektContext.injektSymbols.givenContext.constructors.single()).apply {
                                        putValueArgument(
                                            0,
                                            irString(key)
                                        )
                                    }
                                }
                            }
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
                            DeclarationGraph.GIVEN_TAG,
                            declaration.getter!!.returnType.uniqueTypeName().asString(),
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
                current.hasAnnotation(InjektFqNames.Module)
            ) return true
            current = current.parent as? IrDeclaration
        }

        return false
    }

}
