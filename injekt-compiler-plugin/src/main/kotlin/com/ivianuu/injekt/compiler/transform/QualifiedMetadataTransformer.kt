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
        ) ?: return super.visitCall(expression)
        context.irTrace.record(InjektWritableSlices.QUALIFIERS, expression, qualifiers)
        return super.visitCall(expression)
    }
}
