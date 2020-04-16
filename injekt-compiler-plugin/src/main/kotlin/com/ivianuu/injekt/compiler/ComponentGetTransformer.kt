package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolve.Key
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentGetTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val component = getTopLevelClass(InjektClassNames.Component)

    override fun visitCall(expression: IrCall): IrExpression {
        return if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get" &&
            expression.symbol.descriptor.typeParameters.single().isReified
        ) {
            DeclarationIrBuilder(pluginContext, expression.symbol).run {
                irCall(
                    callee = symbolTable.referenceSimpleFunction(
                        component.unsubstitutedMemberScope
                            .findSingleFunction(Name.identifier("get")),
                    ),
                    type = expression.getTypeArgument(0)!!
                ).apply {
                    dispatchReceiver = expression.extensionReceiver

                    putValueArgument(
                        0,
                        irString(
                            Key(expression.getTypeArgument(0)!!.toKotlinType())
                                .keyConstant
                        )
                    )
                }
            }
        } else {
            super.visitCall(expression)
        }
    }

}
