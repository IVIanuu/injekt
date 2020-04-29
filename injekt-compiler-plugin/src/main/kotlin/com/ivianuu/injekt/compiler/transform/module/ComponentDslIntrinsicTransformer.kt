package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentDslIntrinsicTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) : AbstractInjektTransformer(context, symbolRemapper, bindingTrace) {

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol.descriptor.fqNameSafe == InjektFqNames.ComponentDslIntrinsic) {
            return expression.getValueArgument(0) ?: error("Expected non-null dsl argument")
        }
        return super.visitCall(expression)
    }

}
