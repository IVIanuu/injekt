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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectCallableFakeOverrideTransformer(
  private val context: InjektContext,
  private val trace: BindingTrace,
  private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
  override fun visitFunction(declaration: IrFunction): IrStatement {
    if (trace.bindingContext[InjektWritableSlices.WAS_FAKE_OVERRIDE, declaration.descriptor] !=
      null) {
      declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).run {
        irBlockBody {
          val superFunction = declaration.getLastOverridden()
          +irCall(
            superFunction,
            superQualifierSymbol = superFunction.parent.cast<IrClass>().symbol
          ).apply {
            for (i in 0 until typeArgumentsCount)
              putTypeArgument(i, declaration.typeParameters[i].defaultType)
            declaration.dispatchReceiverParameter?.let {
              dispatchReceiver = irGet(it)
            }
            declaration.extensionReceiverParameter?.let {
              extensionReceiver = irGet(it)
            }
            for (i in 0 until valueArgumentsCount)
              putValueArgument(i, irGet(declaration.valueParameters[i]))
          }
        }
      }
    }
    return super.visitFunction(declaration)
  }
}
