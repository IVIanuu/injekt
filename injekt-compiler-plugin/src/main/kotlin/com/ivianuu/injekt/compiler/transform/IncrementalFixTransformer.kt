/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class IncrementalFixTransformer(
    private val context: InjektContext,
    private val trace: BindingTrace,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
    private val filesWithGivens = mutableSetOf<IrFile>()
    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)
        if (declaration !in filesWithGivens) return declaration

        val clazz = IrFactoryImpl.buildClass {
            name = "${pluginContext.moduleDescriptor.name
                .asString().replace("<", "")
                .replace(">", "")}_${declaration.fileEntry.name.removeSuffix(".kt")
                .substringAfterLast(".")
                .substringAfterLast("/")
                }_GivensMarker".asNameId()
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            parent = declaration
            declaration.addChild(this)
        }

        val function = IrFunctionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(
                object : WrappedSimpleFunctionDescriptor() {
                    override fun hasStableParameterNames(): Boolean = true
                }
            ),
            "givens".asNameId(),
            DescriptorVisibilities.PUBLIC,
            Modality.FINAL,
            pluginContext.irBuiltIns.unitType,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        ).apply {
            descriptor.cast<WrappedSimpleFunctionDescriptor>().bind(this)
            parent = declaration
            declaration.addChild(this)
            addValueParameter("marker", clazz.defaultType)
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
            }
        }

        declaration.metadata = DescriptorMetadataSource.File(
            declaration.metadata.cast<DescriptorMetadataSource.File>()
                .descriptors + function.descriptor
        )

        return declaration
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration is IrDeclarationWithVisibility &&
            (declaration.visibility == DescriptorVisibilities.PUBLIC ||
                    declaration.visibility == DescriptorVisibilities.INTERNAL ||
                    declaration.visibility == DescriptorVisibilities.PROTECTED) &&
                declaration.descriptor.isGiven(context, trace)) {
            filesWithGivens += declaration.file
        }
        return super.visitDeclaration(declaration)
    }
}
