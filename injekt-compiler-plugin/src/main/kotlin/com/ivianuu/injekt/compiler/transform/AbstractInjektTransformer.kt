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

import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
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
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractInjektTransformer(
    protected val context: IrPluginContext
) : IrElementTransformerVoid() {

    protected val symbols = InjektSymbols(context)
    protected val symbolTable = context.symbolTable
    private val typeTranslator = context.typeTranslator
    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

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
                symbols.injektInternalPackage.memberScope
                    .findFirstFunction("stub") { it.typeParameters.isEmpty() }
            ),
            this@AbstractInjektTransformer.context.irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.bindingMetadata(qualifiers: List<FqName>): IrConstructorCall {
        return irCallConstructor(
            symbols.bindingMetadata.constructors.single(),
            emptyList()
        ).apply {
            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@AbstractInjektTransformer.context.irBuiltIns.arrayClass
                        .typeWith(this@AbstractInjektTransformer.context.irBuiltIns.stringType),
                    this@AbstractInjektTransformer.context.irBuiltIns.stringType,
                    qualifiers.map { irString(it.asString()) }
                )
            )
        }
    }

    fun IrBuilderWithScope.providerMetadata(isSingle: Boolean): IrConstructorCall {
        return irCallConstructor(
            symbols.providerMetadata.constructors.single(),
            emptyList()
        ).apply {
            putValueArgument(
                0,
                irBoolean(isSingle)
            )
        }
    }

    fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
        +IrDelegatingConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            symbolTable.referenceConstructor(
                context.builtIns.any
                    .unsubstitutedPrimaryConstructor!!
            )
        )
        +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            context.irBuiltIns.unitType
        )
    }

}

object InjektDeclarationOrigin : IrDeclarationOrigin
object InjektStatementOrigin : IrStatementOrigin
