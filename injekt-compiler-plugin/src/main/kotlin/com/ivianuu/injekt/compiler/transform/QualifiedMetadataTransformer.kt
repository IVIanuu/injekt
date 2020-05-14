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

import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.QualifiedExpressionsStore
import com.ivianuu.injekt.compiler.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

class QualifiedMetadataTransformer(context: IrPluginContext) :
    AbstractInjektTransformer(context) {
    private val fileStack = mutableListOf<IrFile>()
    override fun visitFile(declaration: IrFile): IrFile {
        fileStack.push(declaration)
        return super.visitFile(declaration)
            .also { fileStack.pop() }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val qualifiers = QualifiedExpressionsStore.getQualifiers(
            fileStack.last().symbol.owner.name, expression.startOffset, expression.endOffset
        )?.mapNotNull {
            InjektDeclarationIrBuilder(pluginContext, expression.symbol)
                .generateAnnotationConstructorCall(it)
        }
            ?: return super.visitCall(expression)
        pluginContext.irTrace.record(InjektWritableSlices.QUALIFIERS, expression, qualifiers)
        return super.visitCall(expression)
    }
}
