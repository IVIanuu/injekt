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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class CompositionMetadataTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private val metadataByFunction = mutableMapOf<IrFunction, IrClass>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val functions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Module) ||
                    declaration.hasAnnotation(InjektFqNames.CompositionFactory)
                ) {
                    functions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        functions.forEach {
            getCompositionMetadata(it)
        }

        return super.visitModuleFragment(declaration)
    }

    fun getCompositionMetadata(function: IrFunction): IrClass? {
        metadataByFunction[function]?.let { return it }

        val compositionTypes = mutableListOf<IrType>()
        val entryPoints = mutableListOf<IrType>()

        function.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when {
                    expression.symbol.descriptor.fqNameSafe.asString() ==
                            "com.ivianuu.injekt.composition.installIn" -> {
                        compositionTypes += expression.getTypeArgument(0)!!
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irUnit()
                    }
                    expression.symbol.descriptor.fqNameSafe.asString() ==
                            "com.ivianuu.injekt.composition.entryPoint" -> {
                        entryPoints += expression.getTypeArgument(0)!!
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irUnit()
                    }
                    else -> super.visitCall(expression)
                }
            }
        })

        if (compositionTypes.isEmpty() && entryPoints.isEmpty()) return null

        val metadata = buildClass {
            this.name = getCompositionMetadataName(function)
            visibility = Visibilities.PUBLIC
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                this.visibility = Visibilities.PUBLIC
            }.apply {
                body = InjektDeclarationIrBuilder(pluginContext, symbol).run {
                    builder.irBlockBody {
                        initializeClassWithAnySuperClass(this@clazz.symbol)
                    }
                }
            }

            annotations += DeclarationIrBuilder(pluginContext, function.symbol)
                .irCall(symbols.astCompositionTypes.constructors.single()).apply {
                    putValueArgument(
                        0,
                        IrVarargImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            irBuiltIns.arrayClass.typeWith(irBuiltIns.kClassClass.starProjectedType),
                            irBuiltIns.kClassClass.starProjectedType,
                            compositionTypes
                                .map {
                                    IrClassReferenceImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        irBuiltIns.kClassClass.typeWith(it),
                                        it.classifierOrFail,
                                        it
                                    )
                                }
                        )
                    )
                }

            annotations += DeclarationIrBuilder(pluginContext, function.symbol)
                .irCall(symbols.astEntryPoints.constructors.single()).apply {
                    putValueArgument(
                        0,
                        IrVarargImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            irBuiltIns.arrayClass.typeWith(irBuiltIns.kClassClass.starProjectedType),
                            irBuiltIns.kClassClass.starProjectedType,
                            entryPoints
                                .map {
                                    IrClassReferenceImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        irBuiltIns.kClassClass.typeWith(it),
                                        it.classifierOrFail,
                                        it
                                    )
                                }
                        )
                    )
                }
        }

        function.file.addChild(metadata)

        metadataByFunction[function] = metadata

        return metadata
    }

}
