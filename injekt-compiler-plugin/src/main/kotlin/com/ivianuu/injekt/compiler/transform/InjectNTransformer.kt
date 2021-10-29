/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.utils.addToStdlib.cast

@OptIn(ObsoleteDescriptorBasedAPI::class) class InjectNTransformer(
  @Inject private val context: InjektContext,
  @Inject private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
  private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

  fun transformIfNeeded(function: IrFunction): IrFunction {
    val info = if (function.descriptor is PropertyAccessorDescriptor)
      function.descriptor.cast<PropertyAccessorDescriptor>().correspondingProperty.callableInfo()
    else function.descriptor.callableInfo()
    if (info.injectNParameters.isEmpty()) return function

    if (function in transformedFunctions.values) return function

    return transformedFunctions.getOrPut(function) {
      info.injectNParameters.forEach { parameter ->
        function.addValueParameter(
          parameter.name,
          parameter.typeRef.toIrType(localClasses = emptyList()).typeOrNull!!
        )
      }
      function
    }
  }

  override fun visitFunctionNew(declaration: IrFunction): IrStatement =
    transformIfNeeded(super.visitFunctionNew(declaration) as IrFunction)

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val callee = transformIfNeeded(expression.symbol.owner)

    if (expression.symbol.owner !in transformedFunctions) return super.visitFunctionAccess(expression)

    return when (expression) {
      is IrCall -> IrCallImpl(
        expression.startOffset,
        expression.endOffset,
        expression.type,
        callee.symbol.cast(),
        expression.typeArgumentsCount,
        callee.valueParameters.size,
        expression.origin,
        expression.superQualifierSymbol
      ).apply {
        copyTypeAndValueArgumentsFrom(expression)
      }
      else -> throw AssertionError()
    }
  }
}
