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
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getTopLevelClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractInjektTransformer(
    protected val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    protected val symbolTable = pluginContext.symbolTable
    protected val typeTranslator = pluginContext.typeTranslator
    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

    protected val injektPackage =
        pluginContext.moduleDescriptor.getPackage(InjektFqNames.InjektPackage)
    protected val injektInternalPackage =
        pluginContext.moduleDescriptor.getPackage(InjektFqNames.InjektInternalPackage)

    protected fun getTopLevelClass(fqName: FqName) =
        pluginContext.moduleDescriptor.getTopLevelClass(fqName)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        return super.visitModuleFragment(declaration)
            .also {
                it.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitFile(declaration: IrFile): IrFile {
                        return super.visitFile(declaration)
                            .also {
                                it as IrFileImpl
                                it.metadata = MetadataSource.File(
                                    declaration
                                        .declarations
                                        .map { it.descriptor }
                                )
                            }
                    }
                })

                it.patchDeclarationParents()
            }
    }

    fun IrBuilderWithScope.irInjektStubUnit(): IrExpression {
        return irCall(
            symbolTable.referenceFunction(
                injektInternalPackage.memberScope
                    .findFirstFunction("stub") { it.typeParameters.isEmpty() }
            ),
            pluginContext.irBuiltIns.unitType
        )
    }

    private val bindingMetadata = getTopLevelClass(InjektFqNames.BindingMetadata)

    fun IrBuilderWithScope.bindingMetadata(qualifiers: List<FqName>): IrConstructorCall {
        return irCallConstructor(
            symbolTable.referenceConstructor(bindingMetadata.constructors.single())
                .ensureBound(pluginContext.irProviders),
            emptyList()
        ).apply {
            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(pluginContext.irBuiltIns.stringType),
                    pluginContext.irBuiltIns.stringType,
                    qualifiers.map { irString(it.asString()) }
                )
            )
        }
    }

}

object InjektDeclarationOrigin : IrDeclarationOrigin
object InjektStatementOrigin : IrStatementOrigin
