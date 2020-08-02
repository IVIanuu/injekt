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

import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RunReaderTransformer(
    pluginContext: IrPluginContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()
    private val newDeclarations = mutableListOf<IrDeclarationWithName>()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                return if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.runReader"
                ) {
                    transformRunReaderCall(
                        expression,
                        currentFile,
                        currentScope!!.irElement as IrDeclarationWithName
                    )
                } else expression
            }
        })

        newDeclarations.forEach {
            (it.parent as IrDeclarationContainer).addChild(it)
            indexer.index(it)
        }
    }

    private fun transformRunReaderCall(
        call: IrCall,
        file: IrFile,
        scope: IrDeclarationWithName
    ): IrExpression {
        val inputs = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        val lambda = (call.getValueArgument(1) as IrFunctionExpression).function

        val name = nameProvider.allocateForGroup(
            "${scope.descriptor.fqNameSafe.pathSegments()
                .joinToString("_")}RunReaderContextImpl".asNameId()
        )
        val index = buildClass {
            this.name = "${name}Index".asNameId()
            kind = ClassKind.INTERFACE
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            parent = file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += lambda.getContext()!!.defaultType
            addMetadataIfNotLocal()
            if (scope is IrTypeParametersContainer)
                copyTypeParametersFrom(scope as IrTypeParametersContainer)
            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.runReaderContext.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(file.fqName.child(name).asString())
                    )
                }
            }

            addFunction {
                this.name = "inputs".asNameId()
                returnType = irBuiltIns.unitType
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                parent = this@clazz
                addMetadataIfNotLocal()
                inputs.forEach {
                    addValueParameter(
                        it.type.uniqueTypeName().asString(),
                        it.type
                    )
                }
            }
        }

        newDeclarations += index

        val contextImplStub = buildClass {
            this.name = name
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            visibility = Visibilities.INTERNAL
            if (inputs.isEmpty()) kind = ClassKind.OBJECT
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()
            parent = IrExternalPackageFragmentImpl(
                IrExternalPackageFragmentSymbolImpl(
                    EmptyPackageFragmentDescriptor(
                        pluginContext.moduleDescriptor,
                        scope.getPackageFragment()!!.fqName
                    )
                ),
                scope.getPackageFragment()!!.fqName
            )

            if (inputs.isNotEmpty()) {
                addConstructor {
                    returnType = defaultType
                    isPrimary = true
                    visibility = Visibilities.PUBLIC
                }.apply {
                    inputs.forEach {
                        addValueParameter(
                            it.type.uniqueTypeName().asString(),
                            it.type
                        )
                    }
                }
            }
        }

        return DeclarationIrBuilder(pluginContext, call.symbol).run {
            irBlock {
                val rawContextExpression = if (inputs.isNotEmpty()) {
                    irCall(contextImplStub.constructors.single()).apply {
                        inputs.forEachIndexed { index, instance ->
                            putValueArgument(index, instance)
                        }
                    }
                } else {
                    irGetObject(contextImplStub.symbol)
                }

                var contextUsageCount = 0
                lambda.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        if (expression.symbol == lambda.valueParameters.last().symbol) {
                            contextUsageCount++
                        }
                        return super.visitGetValue(expression)
                    }
                })

                val contextExpression: () -> IrExpression = if (contextUsageCount > 1) {
                    val tmp = irTemporary(rawContextExpression);
                    { irGet(tmp) }
                } else {
                    { rawContextExpression }
                }

                (lambda.body as IrBlockBody).statements.forEach { stmt ->
                    +stmt.transform(
                        object : IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                return if (expression.symbol == lambda.valueParameters.last().symbol)
                                    contextExpression()
                                else super.visitGetValue(expression)
                            }

                            override fun visitReturn(expression: IrReturn): IrExpression {
                                val result = super.visitReturn(expression) as IrReturn
                                return if (result.returnTargetSymbol == lambda.symbol) result.value else result
                            }
                        },
                        null
                    )
                }
            }
        }
    }

}
