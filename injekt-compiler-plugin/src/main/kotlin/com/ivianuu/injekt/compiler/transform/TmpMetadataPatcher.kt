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

import com.ivianuu.injekt.compiler.InjektWritableSlices
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// todo once we can use FIR
class TmpMetadataPatcher(injektContext: InjektContext) :
    AbstractInjektTransformer(injektContext) {

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFile(declaration: IrFile): IrFile {
                (declaration as IrFileImpl).metadata =
                    MetadataSource.File(
                        ((declaration.metadata as MetadataSource.File).descriptors + (declaration.declarations
                            .filterIsInstance<IrDeclarationWithName>()
                            .filter {
                                (it !is IrSimpleFunction ||
                                        injektContext.irTrace[InjektWritableSlices.IS_TRANSFORMED_IMPLICIT_FUNCTION, it] != true)
                            })
                            .map { it.descriptor })
                            .distinct()
                    )
                return declaration
            }
        })
    }

}
