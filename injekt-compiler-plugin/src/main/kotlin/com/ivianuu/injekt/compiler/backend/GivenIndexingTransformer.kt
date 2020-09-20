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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

@Given
class GivenIndexingTransformer : IrLowering {

    override fun lower() {
        val indexer = given<Indexer>()
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) ||
                    declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)
                ) {
                    indexer.index(
                        declaration.constructedClass,
                        currentFile
                    )
                }
                return super.visitConstructor(declaration)
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (!declaration.isInGivenSet()) {
                    when {
                        declaration.hasAnnotation(InjektFqNames.Given) -> {
                            val explicitParameters = declaration.valueParameters
                                .filter { it != declaration.getContextValueParameter() }
                            val typePath =
                                if (explicitParameters.isEmpty()) declaration.returnType.uniqueTypeName()
                                    .asString()
                                else pluginContext.tmpFunction(explicitParameters.size)
                                    .owner
                                    .typeWith(explicitParameters.map { it.type } + declaration.returnType)
                                    .uniqueTypeName()
                                    .asString()
                            indexer.index(declaration, currentFile)
                        }
                        declaration.hasAnnotation(InjektFqNames.GivenMapEntries) -> {
                            indexer.index(declaration, currentFile)
                        }
                        declaration.hasAnnotation(InjektFqNames.GivenSetElements) -> {
                            indexer.index(declaration, currentFile)
                        }
                    }
                }
                return super.visitFunctionNew(declaration)
            }

            override fun visitClassNew(declaration: IrClass): IrStatement {
                when {
                    declaration.hasAnnotation(InjektFqNames.Given) ||
                            declaration.hasAnnotatedAnnotations(InjektFqNames.Effect) -> {
                        val readerConstructor =
                            declaration.getReaderConstructor()!!
                        val explicitParameters = readerConstructor.valueParameters
                            .filter { it != readerConstructor.getContextValueParameter() }
                        val typePath =
                            if (explicitParameters.isEmpty()) readerConstructor.returnType.uniqueTypeName()
                                .asString()
                            else pluginContext.tmpFunction(explicitParameters.size)
                                .owner
                                .typeWith(explicitParameters.map { it.type } + readerConstructor.returnType)
                                .uniqueTypeName()
                                .asString()
                        indexer.index(declaration, currentFile)
                    }
                }
                return super.visitClassNew(declaration)
            }

            override fun visitPropertyNew(declaration: IrProperty): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Given) &&
                    !declaration.isInGivenSet()
                ) {
                    indexer.index(declaration, currentFile)
                }
                return super.visitPropertyNew(declaration)
            }
        })
    }

    private fun IrDeclaration.isInGivenSet(): Boolean {
        var current: IrDeclaration? = parent as? IrDeclaration

        while (current != null) {
            if (current.hasAnnotation(InjektFqNames.GivenSet)) return true
            current = current.parent as? IrDeclaration
        }

        return false
    }

}
