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
import com.ivianuu.injekt.compiler.generator.asNameId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class CreateTransformer(
    private val context: IrPluginContext,
) : IrElementTransformerVoid() {

    private var scope: IrDeclarationWithName? = null
    private inline fun <R> inScope(scope: IrDeclarationWithName, block: () -> R): R {
        val prevScope = scope
        this.scope = scope
        val result = block()
        this.scope = prevScope
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement =
        inScope(declaration) { super.visitClass(declaration) }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        return if (declaration.name.isSpecial) super.visitSimpleFunction(declaration)
        else inScope(declaration) { super.visitSimpleFunction(declaration) }
    }

    override fun visitProperty(declaration: IrProperty): IrStatement =
        inScope(declaration) { super.visitProperty(declaration) }

    override fun visitCall(expression: IrCall): IrExpression {
        val result = super.visitCall(expression) as IrCall
        return if (result.symbol.descriptor.fqNameSafe == InjektFqNames.create) {
            val componentName = scope!!.file.fqName
                .child("${
                    scope!!.descriptor.fqNameSafe.pathSegments().joinToString("_")
                }_${expression.startOffset}".asNameId())
            val componentImplClassStub = IrFactoryImpl.buildClass {
                this.name = componentName.shortName()
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            }.apply clazz@{
                createImplicitParameterDeclarationWithWrappedDescriptor()
                parent = IrExternalPackageFragmentImpl(
                    IrExternalPackageFragmentSymbolImpl(
                        EmptyPackageFragmentDescriptor(
                            context.moduleDescriptor,
                            componentName.parent()
                        )
                    ),
                    componentName.parent()
                )
            }

            val arguments = (expression.getValueArgument(0) as? IrVarargImpl)
                ?.elements ?: emptyList()

            val componentImplConstructorStub = componentImplClassStub.addConstructor {
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            }.apply {
                parent = componentImplClassStub
                arguments
                    .map { it as IrExpression }
                    .forEachIndexed { index, arg ->
                        addValueParameter(index.toString(), arg.type)
                    }
            }

            val componentInit = IrConstructorCallImpl(
                result.startOffset,
                result.endOffset,
                componentImplClassStub.defaultType,
                componentImplConstructorStub.symbol,
                0,
                0,
                componentImplConstructorStub.valueParameters.size,
                null
            ).apply {
                arguments
                    .map { it as IrExpression }
                    .forEachIndexed { index, argument ->
                        putValueArgument(index, argument)
                    }
            }

            if (expression.getTypeArgument(0)
                !!.classOrNull!!.owner.let {
                    it.hasAnnotation(InjektFqNames.Component) ||
                            it.hasAnnotation(InjektFqNames.MergeComponent)
                }
            ) {
                componentInit
            } else {
                IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    context.irBuiltIns.function(0)
                        .functions
                        .first { it.owner.name.asString() == "invoke" },
                    null,
                    null
                ).apply {
                    dispatchReceiver = componentInit
                }
            }
        } else result
    }
}
