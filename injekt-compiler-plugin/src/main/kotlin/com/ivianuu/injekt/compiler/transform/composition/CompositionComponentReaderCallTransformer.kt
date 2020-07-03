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

import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class CompositionComponentReaderCallTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    private data class ReaderCall(
        val call: IrCall,
        val scope: ScopeWithIr,
        val file: IrFile
    )

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val readerCalls = mutableListOf<ReaderCall>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.composition.reader"
                ) {
                    readerCalls += ReaderCall(
                        expression,
                        currentScope!!,
                        currentFile
                    )
                }
                return super.visitCall(expression)
            }
        })

        val newExpressionsByCall = mutableMapOf<IrCall, IrExpression>()

        readerCalls.forEach { (call, scope, file) ->
            val compositionType = call.extensionReceiver!!.type

            val contextTypes = mutableSetOf<IrType>()

            fun collectContextTypes(
                superClass: IrClass,
                typeArguments: List<IrType>
            ) {
                if (superClass.defaultType in contextTypes) return
                contextTypes += superClass.typeWith(typeArguments)
                superClass.superTypes
                    .map { it to it.classOrNull?.owner }
                    .forEach { (superType, clazz) ->
                        if (clazz != null)
                            collectContextTypes(
                                clazz,
                                superType.typeArguments.map { it.typeOrFail })
                    }
            }

            val contextType =
                call.getValueArgument(0)!!.type.typeArguments.first().typeOrFail

            collectContextTypes(contextType.classOrNull!!.owner, emptyList())

            file.addChild(
                InjektDeclarationIrBuilder(pluginContext, file.symbol)
                    .entryPointModule(
                        nameProvider.allocateForGroup(
                            getJoinedName(
                                file.fqName,
                                scope.scope.scopeOwner.fqNameSafe.parent()
                                    .child(scope.scope.scopeOwner.name.asString() + "Reader")
                            )
                        ),
                        compositionType,
                        contextTypes.toList()
                    )
            )

            newExpressionsByCall[call] =
                DeclarationIrBuilder(pluginContext, call.symbol).run {
                    irCall(
                        pluginContext.referenceFunctions(
                            FqName("com.ivianuu.injekt.composition.reader")
                        ).single { it.owner.extensionReceiverParameter == null }
                    ).apply {
                        putValueArgument(
                            0,
                            call.extensionReceiver
                        )
                        putValueArgument(
                            1,
                            call.getValueArgument(0)!!
                        )
                    }
                }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression =
                newExpressionsByCall[expression]
                    ?: super.visitCall(expression)
        })

        return super.visitModuleFragment(declaration)
    }

}
