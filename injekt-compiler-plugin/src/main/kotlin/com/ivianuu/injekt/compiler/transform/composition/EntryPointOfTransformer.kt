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
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektOrigin
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class EntryPointOfTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    private data class EntryPointOfCall(
        val call: IrCall,
        val scope: ScopeWithIr,
        val file: IrFile
    )

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val entryPointOfCalls = mutableListOf<EntryPointOfCall>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.composition.entryPointOf"
                )
                    return super.visitCall(expression)
                entryPointOfCalls += EntryPointOfCall(
                    expression,
                    currentScope!!,
                    currentFile
                )
                return DeclarationIrBuilder(pluginContext, expression.symbol)
                    .irImplicitCast(
                        expression.getValueArgument(0)!!,
                        expression.getTypeArgument(0)!!
                    )
            }
        })

        entryPointOfCalls.forEach { (call, scope, file) ->
            file.addChild(
                InjektDeclarationIrBuilder(pluginContext, file.symbol).entryPointModule(
                    nameProvider.allocateForGroup(
                        getJoinedName(
                            file.fqName,
                            scope.scope.scopeOwner.fqNameSafe.parent()
                                .child(scope.scope.scopeOwner.name.asString() + "EntryPointModule")
                        )
                    ),
                    call.getValueArgument(0)!!.type,
                    listOf(call.getTypeArgument(0)!!)
                )
            )
        }

        return super.visitModuleFragment(declaration)
    }

}
