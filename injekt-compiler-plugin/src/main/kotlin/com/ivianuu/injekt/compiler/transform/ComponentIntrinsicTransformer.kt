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
import com.ivianuu.injekt.compiler.generator.toComponentImplFqName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentIntrinsicTransformer(
    private val context: IrPluginContext,
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        val result = super.visitCall(expression) as IrCall
        if (result.symbol.descriptor.fqNameSafe == InjektFqNames.ComponentFun) {
            val componentClass = result.getTypeArgument(0)!!.classOrNull!!.owner
            val componentImplFqName =
                componentClass.descriptor.fqNameSafe.toComponentImplFqName()
            val componentImplClassStub = IrFactoryImpl.buildClass {
                this.name = componentImplFqName.shortName()
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            }.apply clazz@{
                createImplicitParameterDeclarationWithWrappedDescriptor()
                parent = IrExternalPackageFragmentImpl(
                    IrExternalPackageFragmentSymbolImpl(
                        EmptyPackageFragmentDescriptor(
                            context.moduleDescriptor,
                            componentImplFqName.parent()
                        )
                    ),
                    componentImplFqName.parent()
                )
            }

            val componentConstructor = componentClass.constructors.singleOrNull()

            val componentImplConstructorStub = componentImplClassStub.addConstructor {
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            }.apply {
                parent = componentImplClassStub
                componentConstructor?.valueParameters?.forEach {
                    addValueParameter(it.name.asString(), it.type)
                }
            }

            return IrConstructorCallImpl(
                result.startOffset,
                result.endOffset,
                result.type,
                componentImplConstructorStub.symbol,
                0,
                0,
                componentImplConstructorStub.valueParameters.size,
                null
            ).apply {
                val arguments = (expression.getValueArgument(0) as? IrVarargImpl)
                    ?.elements ?: emptyList()

                arguments
                    .map { it as IrExpression }
                    .forEachIndexed { index, argument ->
                        putValueArgument(index, argument)
                    }
            }
        }
        return result
    }
}
