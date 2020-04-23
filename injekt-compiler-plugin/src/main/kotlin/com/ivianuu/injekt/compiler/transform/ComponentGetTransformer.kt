package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.resolve.Key
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ComponentGetTransformer(context: IrPluginContext) :
    AbstractInjektTransformer(context) {

    override fun visitCall(expression: IrCall): IrExpression {
        return if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get" &&
            expression.symbol.descriptor.typeParameters.single().isReified
        ) {
            DeclarationIrBuilder(context, expression.symbol).run {
                irCall(
                    symbolTable.referenceSimpleFunction(
                        (if (expression.symbol.owner.extensionReceiverParameter?.type == symbols.component.defaultType)
                            symbols.component.descriptor.unsubstitutedMemberScope
                        else symbols.componentOwner.descriptor.unsubstitutedMemberScope)
                            .findSingleFunction(Name.identifier("get")),
                    ),
                    expression.getTypeArgument(0)!!
                ).apply {
                    dispatchReceiver = expression.extensionReceiver

                    putValueArgument(
                        0,
                        irInt(
                            Key(
                                expression.getTypeArgument(0)!!,
                                expression.getValueArgument(0)
                                    ?.safeAs<IrVarargImpl>()
                                    ?.elements
                                    ?.filterIsInstance<IrGetObjectValue>()
                                    ?.map {
                                        it.type.classOrNull!!.descriptor.containingDeclaration
                                            .fqNameSafe
                                    } ?: emptyList()
                            ).hashCode()
                        )
                    )
                }
            }
        } else {
            super.visitCall(expression)
        }
    }

}
