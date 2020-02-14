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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.Name

class DefinitionCallTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    private val component = getTopLevelClass(InjektClassNames.Component)

    private val definitionStack = mutableListOf<IrClass>()

    override fun visitClassNew(declaration: IrClass): IrStatement {
        try {
            definitionStack.push(declaration)
            return super.visitClassNew(declaration)
        } finally {
            definitionStack.pop()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        println("visit call ${expression.dump()}")
        val definition = definitionStack.lastOrNull() ?: return super.visitCall(expression)

        val componentGet = component.unsubstitutedMemberScope
            .getContributedFunctions(
                Name.identifier("get"),
                NoLookupLocation.FROM_BACKEND
            )
        if (componentGet.none { it == expression.symbol.descriptor }) {
            return super.visitCall(expression)
        }

        super.visitCall(expression)

        /*if (definition.fields.none { it.name.asString() == "init" }) {
            definition.addField(
                fieldName = "init",
                fieldType = context.irBuiltIns.booleanType,
                fieldVisibility = Visibilities.PRIVATE
            ).apply {
                initializer = DeclarationIrBuilder(context, definition.symbol).irExprBody(
                    DeclarationIrBuilder(context, definition.symbol).irBoolean(true)
                )
            }
        }

        definition.addField(
            fieldName = "provider${expression.startOffset + expression.endOffset}",
            fieldType = KotlinTypeFactory.simpleType(
                provider.defaultType,
                arguments = listOf(
                    expression.type.toKotlinType().asTypeProjection()
                )
            ).toIrType().makeNullable(),
            fieldVisibility = Visibilities.PRIVATE
        ).apply {
            initializer = DeclarationIrBuilder(context, definition.symbol).irExprBody(
                DeclarationIrBuilder(context, definition.symbol).irNull()
            )
        }*/

        return expression
    }

}