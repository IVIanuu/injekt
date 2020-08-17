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
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderContextCallTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {

    private val newIndexBuilders = mutableListOf<NewIndexBuilder>()

    private data class NewIndexBuilder(
        val originatingDeclaration: IrDeclarationWithName,
        val classBuilder: IrClass.() -> Unit
    )

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

        newIndexBuilders.forEach {
            indexer.index(
                it.originatingDeclaration,
                listOf(DeclarationGraph.ROOT_CONTEXT_PATH),
                it.classBuilder
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

        val name = injektContext.uniqueClassNameProvider(
            "${scope.descriptor.fqNameSafe.pathSegments()
                .joinToString("_")}ReaderContextFactory".asNameId(),
            file.fqName
        )

        newIndexBuilders += NewIndexBuilder(scope) clazz@{
            superTypes += context.defaultType // todo
            addMetadataIfNotLocal()
            if (scope is IrTypeParametersContainer)
                copyTypeParametersFrom(scope as IrTypeParametersContainer)
            annotations += DeclarationIrBuilder(injektContext, symbol).run {
                irCall(injektContext.injektSymbols.rootContext.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(file.fqName.child(name).asString())
                    )
                }
            }

            addFunction {
                this.name = "inputs".asNameId()
                returnType = injektContext.irBuiltIns.unitType
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
        }

        val contextFactoryStub = buildClass {
            this.name = name
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

        val createFunctionStub = contextFactoryStub.addFunction {
            this.name = "create".asNameId()
            returnType = context.defaultType
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        }.apply {
            dispatchReceiverParameter = contextFactoryStub.thisReceiver!!.copyTo(this)

            inputs.forEach { input ->
                addValueParameter(
                    input.type.uniqueTypeName().asString(),
                    input.type
                )
            }
        }

        return DeclarationIrBuilder(injektContext, call.symbol).run {
            irCall(createFunctionStub).apply {
                dispatchReceiver = irGetObject(contextFactoryStub.symbol)
                inputs.forEachIndexed { index, input ->
                    putValueArgument(index, input)
                }
            }
        }
    }

}
