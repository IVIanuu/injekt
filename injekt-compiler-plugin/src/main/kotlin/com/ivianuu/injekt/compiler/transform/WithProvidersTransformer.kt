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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*

class WithProvidersTransformer : IrElementTransformerVoid() {
  override fun visitCall(expression: IrCall): IrExpression {
    if (expression.symbol.descriptor.fqNameSafe == InjektFqNames.withProviders) {
      // we just remove the import paths parameter to make sure that they don't exist anymore
      // and then we let the inline lowering handle the rest
      expression.putValueArgument(0, null)
    }
    return super.visitCall(expression)
  }
}
