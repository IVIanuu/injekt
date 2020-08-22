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

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.reader.createContextFactory
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RootContextCallTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {

    private val newRootFactories = mutableListOf<IrClass>()

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                return if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.rootContext"
                ) {
                    transformContextCall(
                        expression,
                        currentFile,
                        currentScope!!.irElement as IrDeclarationWithName
                    )
                } else expression
            }
        })

        newRootFactories.forEach {
            (it.parent as IrDeclarationContainer).addChild(it)
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
        val inputs = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        val context = call.getTypeArgument(0)!!.classOrNull!!.owner

        val contextFactory = createContextFactory(
            context = context,
            file = file,
            inputTypes = inputs.map { it.type },
            injektContext = injektContext,
            isChild = false
        ).also { newRootFactories += it }

        return DeclarationIrBuilder(injektContext, call.symbol).run {
            irCall(contextFactory.functions.single()).apply {
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
                dispatchReceiver = irGetObject(contextFactoryImplStub.symbol)
                inputs.forEachIndexed { index, input ->
                    putValueArgument(index, input)
                }
            }
        }
    }

}
