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

import com.ivianuu.injekt.compiler.SimpleUniqueNameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.irClassReference
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderContextCallTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {

    private val newDeclarations = mutableListOf<IrDeclarationWithName>()
    private val newRootFactories = mutableListOf<IrClass>()

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                return if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.rootContext" ||
                    expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.childContext"
                ) {
                    transformContextCall(
                        expression,
                        currentFile,
                        currentScope!!.irElement as IrDeclarationWithName
                    )
                } else expression
            }
        })

        newDeclarations.forEach {
            (it.parent as IrDeclarationContainer).addChild(it)
        }

        newRootFactories.forEach {
            indexer.index(
                listOf(DeclarationGraph.ROOT_CONTEXT_FACTORY_PATH),
                it
            )
        }
    }

    private fun transformContextCall(
        call: IrCall,
        file: IrFile,
        scope: IrDeclarationWithName
    ): IrExpression {
        val contextName = (call.getValueArgument(1) as? IrClassReference)
            ?.classType
            ?.takeUnless { it.isNothing() }
            ?.classOrNull
            ?.owner

        val inputs = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        val context = call.type.classOrNull!!.owner

        val isChild = call.symbol.descriptor.fqNameSafe.asString() ==
                "com.ivianuu.injekt.childContext"

        val contextFactory = buildClass {
            name = injektContext.uniqueClassNameProvider(
                "${contextName?.name ?: context.name}Factory".asNameId(),
                file.fqName
            )
            kind = ClassKind.INTERFACE
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            parent = file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()

            addFunction {
                this.name = "create".asNameId()
                returnType = context.defaultType
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                parent = this@clazz
                addMetadataIfNotLocal()
                val parameterUniqueNameProvider = SimpleUniqueNameProvider()
                inputs.forEach {
                    addValueParameter(
                        parameterUniqueNameProvider(it.type.uniqueTypeName()).asString(),
                        it.type
                    )
                }
            }

            annotations += DeclarationIrBuilder(injektContext, symbol).run {
                if (!isChild) {
                    irCall(injektContext.injektSymbols.rootContextFactory.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irString(
                                file.fqName.child((name.asString() + "Impl").asNameId()).asString()
                            )
                        )
                        if (contextName != null) {
                            putValueArgument(
                                1,
                                irClassReference(contextName)
                            )
                        }
                    }
                } else {
                    irCall(injektContext.injektSymbols.childContextFactory.constructors.single()).apply {
                        if (contextName != null) {
                            putValueArgument(
                                0,
                                irClassReference(contextName)
                            )
                        }
                    }
                }
            }

            newDeclarations += this
            if (!isChild) newRootFactories += this
        }

        return DeclarationIrBuilder(injektContext, call.symbol).run {
            val factoryExpression = if (!isChild) {
                val contextFactoryImplStub = buildClass {
                    this.name = (contextFactory.name.asString() + "Impl").asNameId()
                    origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                    kind = ClassKind.OBJECT
                    visibility = Visibilities.INTERNAL
                }.apply clazz@{
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    parent = IrExternalPackageFragmentImpl(
                        IrExternalPackageFragmentSymbolImpl(
                            EmptyPackageFragmentDescriptor(
                                injektContext.moduleDescriptor,
                                scope.getPackageFragment()!!.fqName
                            )
                        ),
                        scope.getPackageFragment()!!.fqName
                    )
                }

                irGetObject(contextFactoryImplStub.symbol)
            } else {
                irCall(
                    injektContext.referenceFunctions(FqName("com.ivianuu.injekt.given"))
                        .single(),
                    contextFactory.defaultType
                ).apply {
                    putTypeArgument(0, contextFactory.defaultType)
                }
            }

            irCall(contextFactory.functions.single()).apply {
                dispatchReceiver = factoryExpression
                inputs.forEachIndexed { index, input ->
                    putValueArgument(index, input)
                }
            }
        }
    }

}
